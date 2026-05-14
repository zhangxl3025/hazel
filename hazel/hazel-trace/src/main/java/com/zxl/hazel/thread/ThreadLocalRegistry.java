package com.zxl.hazel.thread;

import java.util.function.Supplier;

/**
 * 线程本地栈注册器（泛型优化版）
 *
 * @author hazel
 */
public class ThreadLocalRegistry {

    @SuppressWarnings("rawtypes")
    private static volatile MyThreadLocal instance;

    @SuppressWarnings("unchecked")
    public static <T> MyThreadLocal<T> getInstance(Supplier<T> supplier) {
        if (instance == null) {
            synchronized (ThreadLocalRegistry.class) {
                if (instance == null) {
                    instance = create(supplier);
                }
            }
        }
        return instance;
    }

    private static <T> MyThreadLocal create(Supplier<T> supplier) {
        // 优先尝试 Netty
        try {
            Class.forName("io.netty.util.concurrent.FastThreadLocal");
            return new NettyThreadLocal<>(supplier);
        } catch (Exception e) {
            // 降级 JDK
            return new JdkThreadLocal<>(supplier);
        }
    }


    public static void main(String[] args) {
        MyThreadLocal<String> instance = ThreadLocalRegistry.getInstance(String::new);
        instance.set("test");
        System.out.println(instance.get());
    }
}