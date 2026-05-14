package com.zxl.hazel.trace.exporter;

import com.zxl.hazel.trace.Span;

/**
 * Span导出器接口：定义Span数据的导出规范（符合OpenTelemetry标准）
 * 
 * <p>支持的导出目标：
 * <ul>
 *   <li>Jaeger</li>
 *   <li>Zipkin</li>
 *   <li>SkyWalking</li>
 *   <li>日志</li>
 *   <li>控制台</li>
 * </ul>
 * 
 * <p><b>使用示例：</b>
 * <pre>{@code
 * // 注册导出器（或使用SPI自动发现）
 * TraceExporterRegistry.register(new LogSpanExporter());
 * TraceExporterRegistry.register(new SkyWalkingSpanExporter("http://oap:12800"));
 * 
 * // Span结束时自动导出（由Tracer.endSpan触发）
 * Tracer.endSpan(span);
 * }</pre>
 * 
 * @author hazel
 */
public interface TraceExporter {
    
    /**
     * 导出器名称
     */
    String name();
    
    /**
     * 导出Span（符合OpenTelemetry标准：每个Span独立导出）
     * 
     * @param span Span对象
     */
    void export(Span span);
    
    /**
     * 是否支持导出（可用于条件判断）
     * 
     * @return true表示可用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 关闭导出器（清理资源）
     */
    default void shutdown() {
        // 默认不做任何操作
    }
}
