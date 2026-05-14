package com.zxl.hazel.thread;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.function.Supplier;

public class NettyThreadLocal<T> implements MyThreadLocal<T> {

    private final FastThreadLocal<T> threadLocal;

    public NettyThreadLocal(Supplier<T> supplier) {
        this.threadLocal = new FastThreadLocal<>() {
            @Override
            protected T initialValue() {
                return supplier.get();
            }
        };
    }


    @Override
    public T get() {
        return threadLocal.get();
    }

    @Override
    public void set(T value) {
        threadLocal.set(value);
    }


    @Override
    public void remove() {
        threadLocal.remove();
    }
}