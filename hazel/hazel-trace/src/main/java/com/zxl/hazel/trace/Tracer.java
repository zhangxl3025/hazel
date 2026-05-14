package com.zxl.hazel.trace;

import com.zxl.hazel.apm.APMContextProcessorRegistry;
import com.zxl.hazel.thread.stack.MyThreadLocalStack;
import com.zxl.hazel.thread.stack.ThreadLocalStackRegistry;
import com.zxl.hazel.trace.exporter.TraceExporterRegistry;
import com.zxl.hazel.trace.propagation.Getter;
import com.zxl.hazel.trace.propagation.Setter;
import com.zxl.hazel.trace.propagation.TraceHeaders;
import com.zxl.hazel.trace.support.MdcHook;
import com.zxl.hazel.trace.support.SpanContextHook;
import com.zxl.hazel.trace.support.SpanInitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;

/**
 * Tracer：分布式链路追踪的核心入口（符合OpenTelemetry标准）
 *
 * <p>提供2层API：
 * <ul>
 *   <li><b>底层API</b>：{@link #startSpan(String)} / {@link #endSpan()} - 手动控制</li>
 *   <li><b>高层API</b>：{@link Traceable @Traceable} - 注解（零侵入，推荐）</li>
 * </ul>
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * // 方式1：底层API（手动控制）
 * Span span = Tracer.startSpan("http-request");
 * try {
 *     Span dbSpan = Tracer.startSpan("db-query");
 *     try {
 *         dbSpan.addTag("db.table", "users");
 *         userRepository.save(user);
 *     } finally {
 *         Tracer.endSpan(dbSpan);
 *     }
 * } finally {
 *     Tracer.endSpan(span);
 * }
 *
 * // 方式2：注解（零侵入，推荐）
 * @Traceable("createUser")
 * public User create(User user) { ... }
 * }</pre>
 *
 * @author hazel
 * @see Span
 * @see Traceable
 */
public class Tracer {

    private static final Logger log = LoggerFactory.getLogger(Tracer.class);

    // ==================== 私有字段 ====================

    private static final MyThreadLocalStack<Span> SPAN_STACK = ThreadLocalStackRegistry.getInstance(() -> Span.NONE);
    public static volatile SpanContextHook hook = initializeHook();

    // ==================== 私有方法 ====================

    private static SpanContextHook initializeHook() {
        if (APMContextProcessorRegistry.getDetectedAPM() != null
                && APMContextProcessorRegistry.getDetectedAPM().shouldEnableMDCHook()) {
            return new MdcHook();
        }
        return null;
    }

    private static Deque<Span> getSpanStack() {
        return SPAN_STACK.getStack();
    }


    /**
     * 初始化 Span 的公共字段
     */
    private static Span completeSpanInit(String traceId, String segmentId, String parentSpanId,
                                         String operationName, int level, String globalTxId, String transactionId) {
        Span span = new Span();
        span.setTraceId(traceId);
        span.setSpanId(SpanInitUtil.generateSpanId());
        span.setSegmentId(segmentId);
        span.setParentSpanId(parentSpanId);
        span.setOperationName(operationName);
        span.setLevel(level);
        span.setStartTime(System.currentTimeMillis());
        span.setTransactionId(transactionId);
        span.setGlobalTxId(globalTxId);
        push(span);
        return span;
    }

    /**
     * SPAN_STACK 发生变化时，通知刷新钩子
     */
    private static void freshActiveSpan(Span span) {
        if (hook != null) {
            hook.freshActiveSpan(span);
        }
    }

    private static Span pop() {
        Deque<Span> stack = getSpanStack();
        if (stack == null || stack.isEmpty()) {
            return Span.NONE;
        }
        Span pop = stack.pop();
        freshActiveSpan(stack.peek());
        return pop;
    }

    private static void push(Span span) {
        getSpanStack().push(span);
        freshActiveSpan(span);
    }

    // ---------- Span 工厂方法 ----------

    private static Span createRoot(String operationName) {
        return createRoot(null, operationName);
    }

    private static Span createRoot(String traceId, String operationName) {
        try {
            String resolvedTraceId = traceId != null ? traceId : SpanInitUtil.generateTraceId();
            return completeSpanInit(resolvedTraceId, SpanInitUtil.generateSegmentId(), null, operationName, 0, null, null);
        } catch (Exception e) {
            return Span.NONE;
        }
    }

    private static Span createChild(Span parentSpan, String operationName) {
        try {
            if (parentSpan != null) {
                return completeSpanInit(parentSpan.getTraceId(),
                        parentSpan.getSegmentId(),
                        parentSpan.getSpanId(),
                        operationName,
                        parentSpan.getLevel() + 1,
                        parentSpan.getGlobalTxId(),
                        parentSpan.getTransactionId());
            } else {
                return createRoot(operationName);
            }
        } catch (Exception e) {
            return Span.NONE;
        }
    }

    // ==================== 公共 API ====================

    // ---------- 自定义span上下文hook----------


    public static void setHook(SpanContextHook customHook) {
        hook = customHook;
    }

    // ---------- Span 栈操作 ----------

    public static void endSpan() {
        try {
            // 出栈 && 结束span
            pop().finish();
        } catch (Exception e) {
            log.debug("Failed to end span: {}", e.getMessage());
        }
    }


    public static Span currentSpan() {
        Deque<Span> stack = getSpanStack();
        if (stack == null || stack.isEmpty()) {
            return Span.NONE;
        }
        return stack.peek();
    }


    // ---------- 上下文传播 ----------

    public static <C> void inject(C carrier, Setter<C> setter) {
        try {
            Span currentSpan = Tracer.currentSpan();
            if (currentSpan != null) {
                // 链路追踪 ID
                setter.set(carrier, TraceHeaders.TRACE_ID, currentSpan.getTraceId());
                setter.set(carrier, TraceHeaders.SPAN_ID, currentSpan.getSpanId());

                // 事务 ID（如果存在）
                if (currentSpan.getGlobalTxId() != null) {
                    setter.set(carrier, TraceHeaders.GLOBAL_TX_ID, currentSpan.getGlobalTxId());
                }
                if (currentSpan.getTransactionId() != null) {
                    setter.set(carrier, TraceHeaders.TRANSACTION_ID, currentSpan.getTransactionId());
                }
            }
        } catch (Exception e) {
            // 异常时不影响主流程
            log.debug("Failed to inject trace context: {}", e.getMessage());
        }
    }

    public static <C> void continued(C carrier, Getter<C> getter, String operationName) {
        try {
            // 提取链路追踪 ID
            String traceId = getter.get(carrier, TraceHeaders.TRACE_ID);
            String parentSpanId = getter.get(carrier, TraceHeaders.SPAN_ID);

            // 提取事务 ID
            String globalTxId = getter.get(carrier, TraceHeaders.GLOBAL_TX_ID);
            String transactionId = getter.get(carrier, TraceHeaders.TRANSACTION_ID);

            // 创建 Span（自动注入当前线程的事务ID）
            Span span = Tracer.continued(traceId, operationName);
            span.setParentSpanId(parentSpanId);

            // 显式设置事务ID（从 Header 中提取的）
            if (globalTxId != null) {
                span.setGlobalTxId(globalTxId);
            }
            if (transactionId != null) {
                span.setTransactionId(transactionId);
            }
            log.info("http request path:{}, traceId={}, globalTxId={}", operationName, traceId, globalTxId);
        } catch (Exception e) {
            // 异常时不影响主流程，创建新 Span
            log.debug("Failed to extract trace context: {}", e.getMessage());
            Tracer.startSpan(operationName);
        }
    }

    // ---------- 链路管理 ----------

    public static Span beginChain(String operationName) {
        try {
            return createRoot(operationName);
        } catch (Exception e) {
            log.error("Failed to begin chain: {}", e.getMessage(), e);
            return Span.NONE;
        }
    }

    /**
     * 创建Span（智能判断：自动创建根Span或子Span）
     *
     * <p>行为：
     * <ul>
     *   <li>如果当前线程没有活跃Span → 创建根Span（生成新traceId）</li>
     *   <li>如果当前线程有活跃Span → 创建子Span（复用traceId）</li>
     * </ul>
     *
     * @param operationName 操作名称（如："http-request"、"db-query"）
     * @return Span实例
     */
    public static Span startSpan(String operationName) {
        try {
            Span parentSpan = currentSpan();

            Span span;
            if (parentSpan == null || parentSpan == Span.NONE) {
                // 创建根Span（自动生成traceId）
                span = createRoot(operationName);
            } else {
                // 创建子Span（复用父Span的traceId）
                span = createChild(parentSpan, operationName);
            }
            return span;
        } catch (Exception e) {
            log.error("Failed to start span: {}", e.getMessage(), e);
            return Span.NONE;
        }
    }


    /**
     * 续传链路（使用已有的traceId创建根Span）
     * 用于跨服务接收端（从HTTP Header/MQ消息中提取traceId后）
     *
     * @param traceId       上游传递的traceId
     * @param operationName 当前操作名称
     * @return 根Span实例
     */
    public static Span continued(String traceId, String operationName) {
        try {
            if (traceId == null) {
                return createRoot(null, operationName);
            }
            return createRoot(traceId, operationName);
        } catch (Exception e) {
            log.error("Failed to continue trace: {}", e.getMessage(), e);
            return Span.NONE;
        }
    }


    /**
     * 清理当前线程的全链路Span
     *
     * <p>行为：
     * <ul>
     *   <li>自动结束所有未完成的Span（从栈顶开始）</li>
     *   <li>刷新缓冲区，导出所有已完成的Span</li>
     *   <li>清理Span上下文</li>
     * </ul>
     *
     * <p>通常在Filter/Interceptor的finally块中调用
     */
    public static void clearChain() {
        try {
            // 自动结束所有未完成的Span（从栈顶开始）
            Span span;
            while ((span = Tracer.currentSpan()) != null && span != Span.NONE) {
                if (!span.isFinished()) {
                    pop().finish();
                }
            }
            // 导出所有待导出的Span
            TraceExporterRegistry.flush();
            // 清理上下文
            SPAN_STACK.remove();
            if (hook != null) {
                hook.onClear();
            }
        } catch (Exception e) {
            // Span 清理时不应该抛出异常影响业务
            log.debug("Failed to clear chain: {}", e.getMessage());
        }
    }

    /**
     * 检查是否有活跃的Span
     *
     * @return true表示有活跃的Span
     */
    public static boolean hasActive() {
        try {
            Span span = Tracer.currentSpan();
            return span != null && span != Span.NONE && span.getTraceId() != null;
        } catch (Exception e) {
            log.debug("Failed to check active span: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置当前Span的事务ID
     *
     * @param transactionId 事务ID
     */
    public static void setCurrentTransactionId(String transactionId) {
        try {
            Span currentSpan = Tracer.currentSpan();
            if (currentSpan != null && currentSpan != Span.NONE) {
                currentSpan.setTransactionId(transactionId);
                if (currentSpan.getGlobalTxId() == null) {
                    currentSpan.setGlobalTxId(transactionId);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to set current transaction id: {}", e.getMessage());
        }
    }

    /**
     * 获取当前traceId
     *
     * @return Trace ID，如果没有活跃的Span则返回null
     */
    public static String getCurrentTraceId() {
        try {
            Span span = currentSpan();
            return span != null && span != Span.NONE ? span.getTraceId() : null;
        } catch (Exception e) {
            log.debug("Failed to get current trace id: {}", e.getMessage());
            return null;
        }
    }
}
