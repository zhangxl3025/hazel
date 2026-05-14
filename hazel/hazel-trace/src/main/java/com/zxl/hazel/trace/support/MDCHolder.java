package com.zxl.hazel.trace.support;

import com.zxl.hazel.ContextHolder;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDC上下文的SPI实现：支持跨线程传递MDC上下文
 * 
 * MDC (Mapped Diagnostic Context) 是Slf4j提供的日志上下文工具
 * 常用于在日志中输出traceId、spanId等链路信息
 * 
 * @author hazel
 */
public class MDCHolder implements ContextHolder<Map<String, String>> {
    

    @Override
    public Map<String, String> capture() {
        // 捕获当前线程的MDC上下文
        return MDC.getCopyOfContextMap();
    }
    
    @Override
    public void restore(Map<String, String> snapshot) {
        // 将捕获的MDC上下文恢复到当前线程
        if (snapshot != null) {
            MDC.setContextMap(snapshot);
        }
    }
    
    @Override
    public void cleanup() {
        // 清理当前线程的MDC上下文
        MDC.clear();
    }
}
