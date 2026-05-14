package com.zxl.hazel.trace;

import com.zxl.hazel.trace.metrics.MetricsHandler;

import java.lang.annotation.*;

/**
 * 链路追踪注解（支持监控埋点）
 *
 * @author hazel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traceable {

    /**
     * 操作名称（默认使用方法名）
     */
    String value() default "";

    /**
     * 标签（key=value对，支持参数占位符）
     * 例如：tags = {"user.id", "{userId}", "order.amount", "{amount}"}
     */
    String[] tags() default {};

    /**
     * 是否记录方法参数（默认true）
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回值（默认false，避免大对象）
     */
    boolean recordResult() default false;

    /**
     * 监控埋点配置
     */
    MetricsConfig metrics() default @MetricsConfig;

    /**
     * 监控埋点配置
     */
    @interface MetricsConfig {
        /**
         * 是否启用监控埋点（默认启用）
         */
        boolean enabled() default true;

        /**
         * 监控指标名称（默认使用 operationName）
         */
        String name() default "";

        /**
         * 监控类型
         */
        MetricsType type() default MetricsType.COUNTER_AND_TIMER;

        /**
         * 需要记录的 tags（用于监控维度）
         * 空数组表示使用所有 tags
         */
        String[] dimensions() default {};

        /**
         * 是否记录异常（默认true）
         */
        boolean recordException() default true;

        /**
         * 异常类型白名单（空表示所有）
         */
        Class<? extends Throwable>[] exceptionWhitelist() default {};

        /**
         * 自定义监控处理器
         */
        Class<? extends MetricsHandler> handler() default MetricsHandler.DefaultMetricsHandler.class;
    }

    /**
     * 监控类型
     */
    enum MetricsType {
        /** 仅计数器（适合纯统计） */
        COUNTER,
        /** 仅耗时记录（适合需要P99的方法） */
        TIMER,
        /** 计数器+耗时（默认，最常用） */
        COUNTER_AND_TIMER,
        /** 仅标记成功/失败（适合健康检查） */
        SUCCESS_FAILURE
    }
}