package com.zxl.hazel.apm;

/**
 * APM 类型枚举
 * 
 * 用于识别当前环境接入的 APM 系统，以便：
 * 1. 决定是否启用 MDC Hook
 * 2. 在 ContextManager 中注册对应的上下文传递策略
 * 3. 接管 APM 无法覆盖的场景（如断链、自定义线程池等）
 * 
 * @author hazel
 */
public enum APMType {
    
    /**
     * 未接入任何 APM
     */
    NONE(null),
    
    /**
     * Apache SkyWalking
     * 检测类: org.apache.skywalking.apm.agent.core.context.ContextManager
     */
    SKYWALKING("org.apache.skywalking.apm.agent.core.context.ContextManager"),
    
    /**
     * OpenTelemetry (标准APM)
     * 检测类: io.opentelemetry.api.trace.Span
     */
    OPENTELEMETRY("io.opentelemetry.api.trace.Span"),
    
    /**
     * Jaeger (Uber开源)
     * 检测类: io.jaegertracing.api_v2.Trace
     */
    JAEGER("io.jaegertracing.api_v2.Trace"),
    
    /**
     * Zipkin/Brave (Twitter开源)
     * 检测类: brave.Tracing
     */
    ZIPKIN("brave.Tracing"),
    
    /**
     * 阿里云 ARMS
     * 检测类: com.alibaba.arms.common.trace.ArmsTrace
     */
    ARMS("com.alibaba.arms.common.trace.ArmsTrace"),
    
    /**
     * AWS X-Ray
     * 检测类: com.amazonaws.xray.AWSXRay
     */
    AWS_XRAY("com.amazonaws.xray.AWSXRay");
    
    /**
     * 用于检测的类名
     */
    private final String detectorClass;
    
    APMType(String detectorClass) {
        this.detectorClass = detectorClass;
    }
    
    /**
     * 获取检测类名
     */
    public String getDetectorClass() {
        return detectorClass;
    }
    
    /**
     * 检测当前环境是否接入了该 APM
     */
    public boolean isPresent() {
        if (detectorClass == null) {
            return false;
        }
        try {
            Class.forName(detectorClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 是否需要启用 MDC Hook
     * 仅在未接入 APM 时才启用，避免 traceId 冲突
     */
    public boolean shouldEnableMDCHook() {
        return this == NONE;
    }



}
