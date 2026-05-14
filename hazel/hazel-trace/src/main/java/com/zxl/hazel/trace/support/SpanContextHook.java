package com.zxl.hazel.trace.support;

import com.zxl.hazel.trace.Span;

/**
 * Span上下文钩子接口
 *
 * <p>用于在Span变化时执行自定义操作（如更新MDC、日志上下文等）
 *
 * <p>用户需自行实现Hook并注册：
 * <pre>
 * // SLF4J MDC实现
 * public class MdcSpanHook implements SpanContextHook {
 *     public void onSpanChanged(Span span) {
 *         if (span != null) {
 *             MDC.put("traceId", span.getTraceId());
 *             MDC.put("spanId", span.getSpanId());
 *         } else {
 *             MDC.clear();
 *         }
 *     }
 *     public void onClear() {
 *         MDC.clear();
 *     }
 * }
 *
 * // 注册Hook
 * SpanContext.setHook(new MdcSpanHook());
 * </pre>
 *
 * @author hazel
 */
public interface SpanContextHook {

    /**
     * Span变化时回调（push/pop/clear）
     **/
    void freshActiveSpan(Span span);

    /**
     * 清理上下文时回调
     */
    void onClear();

}
