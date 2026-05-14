package com.zxl.hazel.trace.exporter;

import com.zxl.hazel.trace.Span;
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
        
        // 输出Span的详细信息（每个Span独立导出）
        log.info("[SPAN] traceId={}, spanId={}, operation={}, parentSpanId={}, duration={}ms, level={}, root={}, tags={}",
                span.getTraceId(),
                span.getSpanId(),
                span.getOperationName(),
                span.getParentSpanId(),
                span.getDuration(),
                span.getLevel(),
                span.isRoot() ? "true" : "false",
                span.getTags().isEmpty() ? "none" : span.getTags());
    }
}
