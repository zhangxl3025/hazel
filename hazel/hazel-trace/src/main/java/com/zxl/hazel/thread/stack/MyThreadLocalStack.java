package com.zxl.hazel.thread.stack;

import java.util.Deque;

public interface MyThreadLocalStack<T> {
    
    Deque<T> getStack();


    void remove();
}