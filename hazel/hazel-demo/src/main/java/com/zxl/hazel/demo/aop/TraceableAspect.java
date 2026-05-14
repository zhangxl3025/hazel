package com.zxl.hazel.demo.aop;

import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Traceable;
import com.zxl.hazel.trace.Tracer;
import com.zxl.hazel.trace.metrics.MetricsContext;
import com.zxl.hazel.trace.metrics.MetricsHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Aspect
@Component
public class TraceableAspect {

    private static final Logger log = LoggerFactory.getLogger(TraceableAspect.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final Map<Class<? extends MetricsHandler>, MetricsHandler> handlers = new ConcurrentHashMap<>();

    @Pointcut("@annotation(traceable)")
    public void traceableMethod(Traceable traceable) {
    }

    @Around(value = "traceableMethod(traceable)", argNames = "joinPoint,traceable")
    public Object aroundTraceable(ProceedingJoinPoint joinPoint, Traceable traceable) throws Throwable {
        String operationName = resolveOperationName(joinPoint, traceable);

        // 1. 创建 Span
        Span span = Tracer.startSpan(operationName);

        // 2. 解析 tags（既用于 Span，也用于监控）
        Map<String, String> tags = resolveTags(joinPoint, traceable);
        tags.forEach(span::addTag);

        // 3. 监控埋点准备
        MetricsContext metricsCtx = null;
        if (traceable.metrics().enabled() && meterRegistry != null) {
            metricsCtx = initMetricsContext(operationName, traceable, tags);
        }

        try {
            // 4. 记录参数
            if (traceable.recordParams()) {
                recordParams(joinPoint);
            }

            // 5. 执行业务
            Object result = joinPoint.proceed();

            // 6. 记录返回值
            if (traceable.recordResult()) {
                span.addTag("return", String.valueOf(result));
            }

            // 7. 标记成功
            if (metricsCtx != null) {
                metricsCtx.setSuccess(true);
            }

            return result;

        } catch (Throwable e) {
            // 8. 标记错误
            markError(span, e);

            if (metricsCtx != null) {
                metricsCtx.setSuccess(false);
                metricsCtx.setThrowable(e);
            }
            throw e;

        } finally {
            // 9. 结束 Span
            Tracer.endSpan();

            // 10. 记录监控埋点
            if (metricsCtx != null) {
                recordMetrics(metricsCtx, traceable);
            }
        }
    }

    /**
     * 解析操作名称
     */
    private String resolveOperationName(ProceedingJoinPoint joinPoint, Traceable traceable) {
        String operationName = traceable.value();
        if (operationName == null || operationName.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            operationName = signature.getMethod().getName();
        }
        return operationName;
    }

    /**
     * 解析 tags（支持参数占位符）
     */
    private Map<String, String> resolveTags(ProceedingJoinPoint joinPoint, Traceable traceable) {
        Map<String, String> tags = new HashMap<>();
        String[] tagPairs = traceable.tags();

        if (tagPairs == null || tagPairs.length == 0) {
            return tags;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        for (int i = 0; i < tagPairs.length; i += 2) {
            if (i + 1 >= tagPairs.length) {
                log.warn("Invalid tags configuration: {}", (Object) tagPairs);
                break;
            }

            String key = tagPairs[i];
            String valueTemplate = tagPairs[i + 1];
            String value = replaceParams(valueTemplate, paramNames, paramValues);
            tags.put(key, value);
        }

        return tags;
    }

    /**
     * 替换参数占位符 {paramName}
     */
    private String replaceParams(String template, String[] paramNames, Object[] paramValues) {
        if (template == null || paramNames == null || paramValues == null) {
            return template;
        }

        String result = template;
        for (int i = 0; i < paramNames.length; i++) {
            String placeholder = "{" + paramNames[i] + "}";
            if (result.contains(placeholder)) {
                String value = formatParamValue(paramValues[i]);
                result = result.replace(placeholder, value);
            }
        }
        return result;
    }

    /**
     * 格式化参数值（防止 null 和文件类型）
     */
    private String formatParamValue(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> paramType = value.getClass();
        // 跳过文件类型
        if (MultipartFile.class.isAssignableFrom(paramType) || paramType.getName().contains("MultipartFile")) {
            return "**[file]**";
        }
        if (value instanceof byte[]) {
            return "**[bytes]**";
        }
        if (value instanceof InputStream) {
            return "**[stream]**";
        }
        return String.valueOf(value);
    }

    /**
     * 初始化监控上下文
     */
    private MetricsContext initMetricsContext(String operationName, Traceable traceable, Map<String, String> tags) {
        MetricsContext ctx = new MetricsContext();
        ctx.setOperationName(operationName);
        ctx.setConfig(traceable.metrics());
        ctx.setOriginalTags(tags);
        ctx.setStartTime(System.currentTimeMillis());
        ctx.setTimerSample(Timer.start(meterRegistry));
        ctx.buildEffectiveDimensions();
        return ctx;
    }

    /**
     * 记录监控埋点
     */
    private void recordMetrics(MetricsContext ctx, Traceable traceable) {
        try {
            ctx.setEndTime(System.currentTimeMillis());
            ctx.setDuration(ctx.getEndTime() - ctx.getStartTime());

            // 获取处理器
            MetricsHandler handler = getMetricsHandler(traceable.metrics().handler());

            // 记录
            handler.record(ctx);

        } catch (Exception e) {
            log.debug("Failed to record metrics: {}", e.getMessage());
        }
    }

    /**
     * 获取监控处理器（带缓存）
     */
    private MetricsHandler getMetricsHandler(Class<? extends MetricsHandler> handlerClass) {
        return handlers.computeIfAbsent(handlerClass, clazz -> {
            try {
                MetricsHandler handler = clazz.getDeclaredConstructor().newInstance();
                handler.init(meterRegistry);
                return handler;
            } catch (Exception e) {
                log.warn("Failed to create metrics handler: {}", handlerClass.getName());
                return new MetricsHandler.DefaultMetricsHandler();
            }
        });
    }

    /**
     * 记录方法参数（用于日志）
     */
    private void recordParams(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        String params = formatParams(paramNames, paramValues);
        log.info("{} [{}]", methodName, params);
    }

    /**
     * 格式化方法参数（过滤文件类型）
     */
    private String formatParams(String[] paramNames, Object[] paramValues) {
        if (paramNames == null || paramValues == null || paramNames.length == 0) {
            return "";
        }

        List<String> params = new ArrayList<>();
        for (int i = 0; i < paramNames.length; i++) {
            String value = formatParamValue(paramValues[i]);
            params.add(paramNames[i] + "=" + value);
        }
        return params.stream().collect(Collectors.joining(", "));
    }

    /**
     * 标记错误（Span 和 Metrics 共用）
     */
    private void markError(Span span, Throwable e) {
        if (span != null && span != Span.NONE) {
            span.addTag("error", "true");
            span.addTag("error.message", e.getMessage());
            span.addTag("error.type", e.getClass().getSimpleName());
        }
    }
}