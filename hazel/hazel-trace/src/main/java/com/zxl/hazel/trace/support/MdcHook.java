package com.zxl.hazel.trace.support;

import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Tracer;
import org.slf4j.MDC;

public class MdcHook implements SpanContextHook {
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_OPERATION = "operation";
    private static final String MDC_SEGMENT_ID = "segmentId";

    private static final String MDC_TRANSACTION_ID = "transactionId";

    @Override
    public void freshActiveSpan(Span span) {
        MDC.put(MDC_TRACE_ID, span.getTraceId());
        MDC.put(MDC_SPAN_ID, span.getSpanId());
        MDC.put(MDC_SEGMENT_ID, span.getSegmentId());
        MDC.put(MDC_OPERATION, span.getOperationName());
        MDC.put(MDC_TRANSACTION_ID, span.getTransactionId());
    }

    @Override
    public void onClear() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_SEGMENT_ID);
        MDC.remove(MDC_OPERATION);
        MDC.remove(MDC_TRANSACTION_ID);
    }

}
