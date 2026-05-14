package com.zxl.hazel.trace.propagation;

/**
 * Trace上下文传播的Key
 * 参考 OpenTelemetry 和主流 APM 的标准 Header 命名
 */
public interface TraceHeaders {
    /** 链路追踪 ID */
    String TRACE_ID = "X-Trace-Id";
    
    /** Span ID */
    String SPAN_ID = "X-Span-Id";
    
    /** 全局事务ID（跨服务的分布式事务） */
    String GLOBAL_TX_ID = "X-Global-Tx-Id";
    
    /** 本地事务ID（当前服务内的事务） */
    String TRANSACTION_ID = "X-Transaction-Id";
}