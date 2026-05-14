package com.zxl.hazel.thread.stack;


import java.util.function.Supplier;

/**
 * 线程本地栈注册器（泛型优化版）
 *
 * @author hazel
 */
public class ThreadLocalStackRegistry {

    @SuppressWarnings("rawtypes")
    private static volatile MyThreadLocalStack instance;

    @SuppressWarnings("unchecked")
    public static <T> MyThreadLocalStack<T> getInstance(Supplier<T> supplier) {
        if (instance == null) {
            synchronized (ThreadLocalStackRegistry.class) {
                if (instance == null) {
                    instance = create(supplier);
                }
            }
        }
        return instance;
    }

    private static <T> MyThreadLocalStack<T> create(Supplier<T> supplier) {
        // 优先尝试 Netty
        try {
            Class.forName("io.netty.util.concurrent.FastThreadLocal");
            return new NettyThreadLocalStack<>(supplier);
        } catch (Exception e) {
            // 降级 JDK
            return new JdkThreadLocalStack<>(supplier);
        }
    }

}