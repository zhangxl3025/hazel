package com.zxl.hazel.trace.support;

import com.zxl.hazel.ContextHolder;
import com.zxl.hazel.thread.stack.ThreadLocalStackRegistry;
import com.zxl.hazel.trace.Span;

/**
 * Span上下文的SPI实现：支持跨线程传递Trace上下文
 *
 * <p>符合OpenTelemetry标准：通过Span传递链路追踪上下文。
 *
 * @author hazel
 */
public class SpanHolder implements ContextHolder<Span> {

    @Override
    public Span capture() {
        return ThreadLocalStackRegistry.getInstance(Span::new).getStack().peek();
    }

    @Override
    public void restore(Span snapshot) {
        if (snapshot != null) {
            ThreadLocalStackRegistry.getInstance(Span::new).getStack().push(snapshot);
        }
    }

    @Override
    public void cleanup() {
        ThreadLocalStackRegistry.getInstance(Span::new).getStack().clear();
    }
}
