package com.zxl.hazel.trace.metrics;

import com.zxl.hazel.trace.Traceable;
import io.micrometer.core.instrument.Timer;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控上下文
 */

public class MetricsContext {
    
    /**
     * 操作名称
     */
    private String operationName;
    
    /**
     * 监控配置
     */
    private Traceable.MetricsConfig config;
    
    /**
     * 原始 tags（从 @Traceable 解析）
     */
    private Map<String, String> originalTags;
    
    /**
     * 实际使用的维度（根据 dimensions 筛选）
     */
    private Map<String, String> effectiveDimensions;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 异常（如果失败）
     */
    private Throwable throwable;
    
    /**
     * 耗时采样器
     */
    private Timer.Sample timerSample;
    
    /**
     * 开始时间
     */
    private long startTime;
    
    /**
     * 结束时间
     */
    private long endTime;
    
    /**
     * 耗时（毫秒）
     */
    private long duration;
    
    public void buildEffectiveDimensions() {
        if (effectiveDimensions == null) {
            effectiveDimensions = new HashMap<>();
        }
        
        String[] dimensions = config.dimensions();
        if (dimensions.length == 0) {
            // 使用所有原始 tags
            effectiveDimensions.putAll(originalTags);
        } else {
            // 只使用指定的维度
            for (String dim : dimensions) {
                if (originalTags.containsKey(dim)) {
                    effectiveDimensions.put(dim, originalTags.get(dim));
                }
            }
        }
        
        // 固定维度
        effectiveDimensions.put("operation", operationName);
        effectiveDimensions.put("success", String.valueOf(success));
    }


    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Traceable.MetricsConfig getConfig() {
        return config;
    }

    public void setConfig(Traceable.MetricsConfig config) {
        this.config = config;
    }

    public Map<String, String> getOriginalTags() {
        return originalTags;
    }

    public void setOriginalTags(Map<String, String> originalTags) {
        this.originalTags = originalTags;
    }

    public Map<String, String> getEffectiveDimensions() {
        return effectiveDimensions;
    }

    public void setEffectiveDimensions(Map<String, String> effectiveDimensions) {
        this.effectiveDimensions = effectiveDimensions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public Timer.Sample getTimerSample() {
        return timerSample;
    }

    public void setTimerSample(Timer.Sample timerSample) {
        this.timerSample = timerSample;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}