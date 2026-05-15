package com.zxl.hazel.trace.exporter;

import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.exporter.TraceExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志导出器：将Span数据输出到日志系统（符合OpenTelemetry标准）
 * 
 * <p>特点：
 * <ul>
 *   <li>结构化JSON格式，便于日志分析</li>
 *   <li>可与ELK、Splunk等日志系统集成</li>
 * </ul>
 * 
 * @author hazel
 */
public class LogTraceExporter implements TraceExporter {
    
    private static final Logger log = LoggerFactory.getLogger(LogTraceExporter.class);
    
    @Override
    public String name() {
        return "log";
    }
    
    @Override
    public void export(Span span) {
        if (span == null || !span.isSampled()) {
            return;
        }

        // 压缩 tags 中的换行符，确保日志单行输出
        String tagsStr = span.getTags().isEmpty() ? "none" : formatTags(span.getTags());

        // 输出Span的详细信息（每个Span独立导出）
        log.info("[SPAN] traceId={}, spanId={}, operation={}, parentSpanId={}, duration={}ms, level={}, root={}, tags={}",
                span.getTraceId(),
                span.getSpanId(),
                span.getOperationName(),
                span.getParentSpanId(),
                span.getDuration(),
                span.getLevel(),
                span.isRoot() ? "true" : "false",
                tagsStr);
    }

    /**
     * 格式化 tags，将值中的换行符和连续空白压缩为单个空格
     */
    private String formatTags(java.util.Map<String, String> tags) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : tags.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append('=');
            String val = entry.getValue();
            if (val != null) {
                // 压缩换行 + 连续空白 → 单空格
                val = val.replaceAll("[\\r\\n]+\\s*", " ").trim();
            }
            sb.append(val);
        }
        sb.append('}');
        return sb.toString();
    }
}
