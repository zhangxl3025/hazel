package com.zxl.hazel.thread.stack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class JdkThreadLocalStack<T> implements MyThreadLocalStack<T> {

    private ThreadLocal<Deque<T>> stack = ThreadLocal.withInitial(ArrayDeque::new);


    public JdkThreadLocalStack(Supplier<T> supplier) {
        this.stack = ThreadLocal.withInitial(() -> {
            Deque<T> deque = new ArrayDeque<>();
            deque.push(supplier.get());
            return deque;
        });
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