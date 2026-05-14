package com.zxl.hazel.thread;

public interface MyThreadLocal<T> {
    
    T get();

    void set(T value);

    void remove();
}