package com.zxl.hazel.trace.exporter;

import com.zxl.hazel.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * SkyWalking Trace 导出器
 *
 * <p>直接上报，不维护自己的缓冲区（由 TraceExporterRegistry 统一批处理）
 *
 * <p>使用方式：
 * <pre>
 * // 方式1：SPI 自动加载（需要无参构造函数）
 * // 在 META-INF/services/com.zxl.hazel.trace.exporter.TraceExporter 中配置
 *
 * // 方式2：手动注册（使用 Builder）
 * SkyWalkingTraceExporter exporter = SkyWalkingTraceExporter.builder()
 *     .oapAddress("http://localhost:12800")
 *     .serviceName("my-service")
 *     .build();
 * TraceExporterRegistry.register(exporter);
 * </pre>
 *
 * @author hazel
 */
public class SkyWalkingTraceExporter implements TraceExporter {

    private static final Logger log = LoggerFactory.getLogger(SkyWalkingTraceExporter.class);

    private String oapAddress = "http://localhost:12800";
    private String serviceName = "unknown-service";
    private String instanceName;
    private String authToken;

    /**
     * 公共无参构造函数（供 SPI 使用）
     */
    public SkyWalkingTraceExporter() {
        initInstanceName();
        log.debug("SkyWalkingTraceExporter created with default config");
    }

    private void initInstanceName() {
        if (instanceName == null) {
            try {
                instanceName = serviceName + "-" + java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                instanceName = serviceName + "-unknown";
            }
        }
    }

    // ========== Setter 方法（供 SPI 或用户配置） ==========

    public void setOapAddress(String oapAddress) {
        this.oapAddress = oapAddress;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        initInstanceName();
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    // ========== TraceExporter 接口实现 ==========

    @Override
    public String name() {
        return "skywalking";
    }

    @Override
    public void export(Span span) {
        if (span == null || !span.isSampled()) {
            return;
        }
        sendSpan(span);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void shutdown() {
        log.info("SkyWalkingTraceExporter shutdown");
    }

    // ========== 内部方法 ==========

    /**
     * 发送单个 Span 到 SkyWalking
     */
    private void sendSpan(Span span) {
        String url = oapAddress + "/v3/traces";
        String body = buildJsonBody(span);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Content-Type", "application/json");

            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authentication", authToken);
            }

            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));

            try (OutputStream out = connection.getOutputStream()) {
                out.write(data);
                out.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                log.debug("Successfully sent span {} to SkyWalking", span.getSpanId());
            } else {
                log.debug("SkyWalking returned error: {}, span dropped", responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            log.debug("Failed to send span to SkyWalking: {}", e.getMessage());
        }
    }

    /**
     * 构建单个 Span 的 JSON
     */
    private String buildJsonBody(Span span) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"service\":\"").append(escapeJson(serviceName)).append("\",");
        sb.append("\"serviceInstance\":\"").append(escapeJson(instanceName)).append("\",");
        sb.append("\"spans\":[");
        sb.append("{");
        sb.append("\"traceId\":\"").append(escapeJson(span.getTraceId())).append("\",");
        sb.append("\"segmentId\":\"").append(escapeJson(span.getSegmentId())).append("\",");
        sb.append("\"spanId\":").append(span.getSpanId()).append(",");

        if (span.getParentSpanId() != null) {
            sb.append("\"parentSpanId\":").append(span.getParentSpanId()).append(",");
        }

        sb.append("\"startTime\":").append(span.getStartTime()).append(",");
        sb.append("\"endTime\":").append(span.getEndTime() > 0 ? span.getEndTime() : System.currentTimeMillis()).append(",");
        sb.append("\"operationName\":\"").append(escapeJson(span.getOperationName())).append("\",");
        sb.append("\"spanType\":").append(determineSpanType(span));

        sb.append("}");
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 确定 Span 类型
     * 1 = Entry（入口），2 = Exit（出口），3 = Local（本地）
     */
    private int determineSpanType(Span span) {
        String opName = span.getOperationName().toLowerCase();
        if (span.getParentSpanId() == null && (opName.startsWith("http") || opName.startsWith("get") || opName.startsWith("post"))) {
            return 1; // Entry
        } else if (opName.contains("http") || opName.contains("rpc") || opName.contains("db") || opName.contains("mq")) {
            return 2; // Exit
        } else {
            return 3; // Local
        }
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ========== Builder 模式（供手动注册使用） ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String oapAddress = "http://localhost:12800";
        private String serviceName = "unknown-service";
        private String instanceName;
        private String authToken;

        public Builder oapAddress(String oapAddress) {
            this.oapAddress = oapAddress;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder instanceName(String instanceName) {
            this.instanceName = instanceName;
            return this;
        }

        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public SkyWalkingTraceExporter build() {
            SkyWalkingTraceExporter exporter = new SkyWalkingTraceExporter();
            exporter.oapAddress = this.oapAddress;
            exporter.serviceName = this.serviceName;
            exporter.authToken = this.authToken;

            if (this.instanceName != null) {
                exporter.instanceName = this.instanceName;
            } else {
                try {
                    exporter.instanceName = exporter.serviceName + "-" + java.net.InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                    exporter.instanceName = exporter.serviceName + "-unknown";
                }
            }

            log.info("SkyWalkingTraceExporter built: oap={}, service={}, instance={}",
                    exporter.oapAddress, exporter.serviceName, exporter.instanceName);
            return exporter;
        }
    }
}