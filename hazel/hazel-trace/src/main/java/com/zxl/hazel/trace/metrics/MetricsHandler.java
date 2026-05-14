package com.zxl.hazel.trace.metrics;

import com.zxl.hazel.trace.Traceable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 监控埋点处理器
 */
public interface MetricsHandler {
    
    /**
     * 初始化
     */
    void init(MeterRegistry registry);
    
    /**
     * 记录监控数据
     * 
     * @param context 监控上下文
     */
    void record(MetricsContext context);
    
    /**
     * 默认实现（基于 Micrometer）
     */
    class DefaultMetricsHandler implements MetricsHandler {
        
        private MeterRegistry registry;
        
        @Override
        public void init(MeterRegistry registry) {
            this.registry = registry;
        }
        
        @Override
        public void record(MetricsContext context) {
            Traceable.MetricsConfig config = context.getConfig();
            String metricName = config.name().isEmpty() 
                ? context.getOperationName() : config.name();
            
            // 构建 tags（维度）
            List<Tag> tags = buildTags(context);
            
            // 根据类型记录
            switch (config.type()) {
                case COUNTER:
                    recordCounter(metricName, tags, context);
                    break;
                case TIMER:
                    recordTimer(metricName, tags, context);
                    break;
                case COUNTER_AND_TIMER:
                    recordCounter(metricName, tags, context);
                    recordTimer(metricName, tags, context);
                    break;
                case SUCCESS_FAILURE:
                    recordSuccessFailure(metricName, tags, context);
                    break;
            }
        }
        
        private void recordCounter(String name, List<Tag> tags, MetricsContext ctx) {
            Counter.builder(name + ".total")
                .tags(tags)
                .register(registry)
                .increment();
                
            if (ctx.isSuccess()) {
                Counter.builder(name + ".success")
                    .tags(tags)
                    .register(registry)
                    .increment();
            } else {
                Counter.builder(name + ".failure")
                    .tags(tags)
                    .register(registry)
                    .increment();
                
                // 异常类型单独计数
                if (ctx.getThrowable() != null) {
                    Counter.builder(name + ".exception")
                        .tags("exception", ctx.getThrowable().getClass().getSimpleName())
                        .tags(tags)
                        .register(registry)
                        .increment();
                }
            }
        }
        
        private void recordTimer(String name, List<Tag> tags, MetricsContext ctx) {
            Timer.Sample sample = ctx.getTimerSample();
            if (sample != null) {
                sample.stop(Timer.builder(name + ".duration")
                    .tags(tags)
                    .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .register(registry));
            }
        }
        
        private void recordSuccessFailure(String name, List<Tag> tags, MetricsContext ctx) {
            String result = ctx.isSuccess() ? "success" : "failure";
            Counter.builder(name + ".result")
                .tag("result", result)
                .tags(tags)
                .register(registry)
                .increment();
        }
        
        private List<Tag> buildTags(MetricsContext ctx) {
            Map<String, String> dimensions = ctx.getEffectiveDimensions();
            List<Tag> tags = new ArrayList<>();
            for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                tags.add(Tag.of(entry.getKey(), entry.getValue()));
            }
            return tags;
        }
    }
}