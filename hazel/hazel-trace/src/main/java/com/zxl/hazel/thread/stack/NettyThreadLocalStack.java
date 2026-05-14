package com.zxl.hazel.thread.stack;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class NettyThreadLocalStack<T> implements MyThreadLocalStack<T> {

    private final FastThreadLocal<Deque<T>> stack;

    public NettyThreadLocalStack(Supplier<T> supplier) {
        this.stack = new FastThreadLocal<Deque<T>>() {
            @Override
            protected Deque<T> initialValue() {
                Deque<T> deque = new ArrayDeque<>();
                deque.push(supplier.get());
                return deque;
            }
        };
    }

    @Override
    public Deque<T> getStack() {
        return stack.get();
    }


    @Override
    public void remove() {
        stack.remove();
    }
}