package com.zxl.hazel.trace;

import com.zxl.hazel.trace.exporter.TraceExporterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Span：链路追踪中的跨度，表示一个操作单元
 *
 * <p>参考标准APM设计：
 * <ul>
 *   <li><b>OpenTelemetry/Zipkin/Jaeger</b>: traceId + spanId + parentSpanId</li>
 *   <li><b>SkyWalking</b>: traceId + spanId + parentSpanId + segmentId</li>
 * </ul>
 *
 * <p>字段说明：
 * <ul>
 *   <li>traceId: 全局唯一的128位标识（32位hex），整个链路唯一</li>
 *   <li>spanId: 全局唯一的64位标识（16位hex），单个操作唯一</li>
 *   <li>parentSpanId: 父span的ID（根span为null）</li>
 *   <li>segmentId: 段ID（SkyWalking概念），表示一个服务实例内的连续调用段（16位hex）</li>
 * </ul>
 *
 * <p>一个Trace由多个Span组成，形成树形结构。
 * 在 SkyWalking 中，一个 segmentId 下的多个 Span 表示同一个服务实例内的连续调用。
 *
 * @author hazel
 */
public class Span {

    /**
     * 空对象,需要时返回此对象避免空指针
     */
    public static final Span NONE = new Span();

    private static final Logger log = LoggerFactory.getLogger(Span.class);

    /**
     * 导出操作的名称常量
     */
    public static final String OPERATION_NAME_EXPORT = "TRACE_EXPORT";

    /**
     * Segment ID（段ID，SkyWalking概念）
     * 表示一个服务实例内的连续调用段，同一线程内的Span共享同一个segmentId
     * 用于 SkyWalking APM 兼容性
     */
    private String segmentId;


    /**
     * Span ID（全局唯一）
     */
    private String spanId;

    /**
     * 父Span ID（根Span为null）
     */
    private String parentSpanId;

    /**
     * Trace ID（整个链路唯一）
     */
    private String traceId;

    /**
     * 操作名称（如：HTTP请求、数据库查询等）
     */
    private String operationName;

    /**
     * 开始时间戳（毫秒）
     */
    private long startTime;

    /**
     * 结束时间戳（毫秒）
     */
    private long endTime;

    /**
     * 是否已完成
     */
    private volatile boolean finished = false;

    /**
     * 采样标记（是否导出此Span）
     */
    private boolean sampled = true;

    /**
     * Span层级（根节点为0）
     */
    private int level = 0;

    /**
     * 全局事务ID（跨服务的分布式事务）
     * 用于跨进程传递，即使在事务未开启时也需要携带
     */
    private String globalTxId;

    /**
     * 本地事务ID（当前服务内的事务）
     * 用于 Span 采样和事务关联
     */
    private String transactionId;

    /**
     * 标签（用于存储额外信息）
     */
    private Map<String, String> tags = new HashMap<>();

    /**
     * 日志（用于存储事件日志）
     */
    private Map<Long, String> logs = new HashMap<>();


    /**
     * 结束Span
     */
    public void finish() {
        finish(System.currentTimeMillis());
    }

    /**
     * 结束Span并指定结束时间
     */
    public void finish(long endTime) {
        try {
            this.endTime = endTime;
            this.finished = true;
        } catch (Exception ignored) {
        }
        // 导出Span（符合OpenTelemetry标准：每个Span独立导出）
        if (!Span.OPERATION_NAME_EXPORT.equals(this.getOperationName())) {
            TraceExporterRegistry.export(this);
        }
    }

    /**
     * 添加标签
     */
    public Span addTag(String key, String value) {
        try {
            this.tags.put(key, value);
        } catch (Exception ignored) {
        }
        return this;
    }

    /**
     * 添加日志
     */
    public Span addLog(String message) {
        try {
            this.logs.put(System.currentTimeMillis(), message);
        } catch (Exception ignored) {
        }
        return this;
    }

    /**
     * 获取执行耗时（毫秒）
     */
    public long getDuration() {
        try {
            if (endTime == 0) {
                return System.currentTimeMillis() - startTime;
            }
            return endTime - startTime;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 判断是否为根Span
     */
    public boolean isRoot() {
        return parentSpanId == null;
    }

    /**
     * 是否被采样
     */
    public boolean isSampled() {
        return sampled;
    }

    /**
     * 设置采样标记
     */
    public void setSampled(boolean sampled) {
        this.sampled = sampled;
    }

    // Getter和Setter

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSegmentId() {
        return segmentId;
    }

    /**
     * 获取全局事务ID
     */
    public String getGlobalTxId() {
        return globalTxId;
    }

    /**
     * 设置全局事务ID
     */
    public void setGlobalTxId(String globalTxId) {
        this.globalTxId = globalTxId;
    }

    /**
     * 获取本地事务ID
     */
    public String getTransactionId() {
        return transactionId;

    }

    /**
     * 设置本地事务ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * 获取本地事务ID
     */
    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<Long, String> getLogs() {
        return logs;
    }

    public void setLogs(Map<Long, String> logs) {
        this.logs = logs;
    }

    @Override
    public String toString() {
        // 结构化输出，适合日志解析和APM可视化
        StringBuilder sb = new StringBuilder();
        sb.append("Span{");
        sb.append("\"traceId\":\"").append(traceId).append("\"");
        sb.append(",\"segmentId\":\"").append(segmentId).append("\"");
        sb.append(",\"spanId\":\"").append(spanId).append("\"");
        if (parentSpanId != null) {
            sb.append(",\"parentSpanId\":\"").append(parentSpanId).append("\"");
        }
        sb.append(",\"operationName\":\"").append(operationName).append("\"");
        sb.append(",\"level\":").append(level);

        // 事务 ID（如果存在）
        if (globalTxId != null) {
            sb.append(",\"globalTxId\":\"").append(globalTxId).append("\"");
        }
        if (transactionId != null) {
            sb.append(",\"transactionId\":\"").append(transactionId).append("\"");
        }

        sb.append(",\"startTime\":").append(startTime);
        if (endTime > 0) {
            sb.append(",\"endTime\":").append(endTime);
        }
        sb.append(",\"duration\":").append(getDuration());
        sb.append(",\"finished\":").append(finished);
        if (!tags.isEmpty()) {
            sb.append(",\"tags\":").append(tags);
        }
        if (!logs.isEmpty()) {
            sb.append(",\"logs\":").append(logs);
        }
        sb.append("}");
        return sb.toString();
    }
}
