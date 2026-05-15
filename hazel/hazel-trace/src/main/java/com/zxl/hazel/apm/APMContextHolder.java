package com.zxl.hazel.apm;

import com.zxl.hazel.context.ContextHolder;
import com.zxl.hazel.context.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APM 上下文 Holder - 将 APM 上下文集成到 ContextManager
 * 
 * <p>这个类实现了 ContextHolder 接口，可以将 APM 的上下文传递能力
 * 集成到 Hazel 的 ContextManager 中，实现跨线程的 APM 上下文传递。
 * 
 * <p>工作原理：
 * <ol>
 *   <li>启动时检测当前环境接入的 APM 类型</li>
 *   <li>从 APMContextProcessorRegistry 获取对应的处理器</li>
 *   <li>通过 ContextManager.wrap() 包装任务，自动传递 APM 上下文</li>
 *   <li>当 APM 无法覆盖的场景（断链、自定义线程池），由 Hazel 接管</li>
 * </ol>
 * 
 * <p>使用示例：
 * <pre>
 * // 1. SPI 自动注册（推荐）
 * // 在 META-INF/services/com.zxl.hazel.context.ContextHolder 中添加：
 * // com.zxl.hazel.trace.APMContextHolder
 * 
 * // 2. 使用 ContextManager 包装任务
 * Runnable task = ContextManager.wrap(() -> {
 *     // 子线程中自动有 APM 上下文
 *     System.out.println(Tracer.currentSpan());
 * });
 * executor.submit(task);
 * </pre>
 * 
 * @author hazel
 * @see ContextManager
 * @see APMContextProcessor
 */
public class APMContextHolder implements ContextHolder<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(APMContextHolder.class);
    
    /**
     * APM 上下文处理器
     */
    private volatile APMContextProcessor processor;
    
    /**
     * 获取或初始化 APM 上下文处理器
     */
    private APMContextProcessor getProcessor() {
        if (processor == null) {
            synchronized (this) {
                if (processor == null) {
                    processor = APMContextProcessorRegistry.getInstance()
                            .getProcessorForCurrentAPM();
                    if (processor != null) {
                        logger.info("APMContextHolder using processor for: {}", 
                                   processor.supportedAPM());
                    } else {
                        logger.debug("No APM processor available");
                    }
                }
            }
        }
        return processor;
    }
    
    @Override
    public Object capture() {
        APMContextProcessor currentProcessor = getProcessor();
        if (currentProcessor == null) {
            logger.debug("No APM processor, skip capture");
            return null;
        }
        
        try {
            return currentProcessor.capture();
        } catch (Exception e) {
            logger.warn("Failed to capture APM context", e);
            return null;
        }
    }
    
    @Override
    public void restore(Object snapshot) {
        APMContextProcessor currentProcessor = getProcessor();
        if (currentProcessor == null) {
            logger.debug("No APM processor, skip restore");
            return;
        }
        
        try {
            currentProcessor.restore(snapshot);
        } catch (Exception e) {
            logger.warn("Failed to restore APM context", e);
        }
    }
    
    @Override
    public void cleanup() {
        APMContextProcessor currentProcessor = getProcessor();
        if (currentProcessor == null) {
            logger.debug("No APM processor, skip cleanup");
            return;
        }
        
        try {
            currentProcessor.cleanup();
        } catch (Exception e) {
            logger.warn("Failed to cleanup APM context", e);
        }
    }
}
