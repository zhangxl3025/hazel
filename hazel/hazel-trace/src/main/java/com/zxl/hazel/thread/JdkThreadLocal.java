package com.zxl.hazel.thread;

import java.util.function.Supplier;

public class JdkThreadLocal<T> implements MyThreadLocal<T> {

    private final ThreadLocal<T> threadLocal;

    public JdkThreadLocal(Supplier<T> supplier) {
        this.threadLocal = ThreadLocal.withInitial(supplier);
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