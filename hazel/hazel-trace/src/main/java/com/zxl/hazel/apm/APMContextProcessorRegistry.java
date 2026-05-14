package com.zxl.hazel.apm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * APM 上下文处理器注册器
 *
 * 负责管理和注册不同 APM 的上下文处理器，将其集成到 ContextManager 中。
 * 当 APM 无法覆盖某些场景时，由 Hazel 接管上下文传递。
 *
 * <p>使用方式：
 * <pre>
 * // 1. SPI 自动注册（推荐）
 * // 在 META-INF/services/com.zxl.hazel.trace.APMContextProcessor 中配置实现类
 * APMContextProcessorRegistry registry = APMContextProcessorRegistry.getInstance();
 *
 * // 2. 手动注册
 * registry.register(new SkyWalkingContextProcessor());
 *
 * // 3. 获取当前 APM 的处理器
 * APMContextProcessor processor = registry.getProcessorForCurrentAPM();
 * </pre>
 *
 * @author hazel
 */
public class APMContextProcessorRegistry {

    private static final Logger log = LoggerFactory.getLogger(APMContextProcessorRegistry.class);

    /**
     * 当前环境检测到的 APM 类型（全局唯一）
     */
    private static final APMType DETECTED_APM = detectAPMType();

    /**
     * 单例实例
     */
    private static volatile APMContextProcessorRegistry instance;

    /**
     * 已注册的处理器映射表 (APMType -> Processor)
     */
    private final ConcurrentMap<APMType, APMContextProcessor> processors = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，通过 SPI 自动加载
     */
    private APMContextProcessorRegistry() {
        loadProcessorsViaSPI();
    }

    /**
     * 获取单例实例
     */
    public static APMContextProcessorRegistry getInstance() {
        if (instance == null) {
            synchronized (APMContextProcessorRegistry.class) {
                if (instance == null) {
                    instance = new APMContextProcessorRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * 获取当前检测到的 APM 类型
     */
    public static APMType getDetectedAPM() {
        return DETECTED_APM;
    }

    /**
     * 检测当前环境接入的 APM 类型
     */
    private static APMType detectAPMType() {
        for (APMType apm : APMType.values()) {
            if (apm.isPresent()) {
                return apm;
            }
        }
        return APMType.NONE;
    }

    /**
     * 通过 SPI 自动加载所有 APMContextProcessor 实现
     * 只会实例化当前环境已接入的 APM 处理器（通过 APMType.isPresent() 判断）
     */
    private void loadProcessorsViaSPI() {
        ServiceLoader<APMContextProcessor> serviceLoader = ServiceLoader.load(APMContextProcessor.class);
        int loadedCount = 0;
        int skippedCount = 0;

        for (APMContextProcessor processor : serviceLoader) {
            APMType apmType = processor.supportedAPM();

            // 关键判断：只有当前环境接入了该 APM 才实例化
            if (apmType.isPresent()) {
                APMContextProcessor previous = register(processor);
                if (previous != null) {
                    log.debug("Overriding {} processor: {} -> {}",
                            apmType, previous.getClass().getSimpleName(), processor.getClass().getSimpleName());
                } else {
                    log.debug("Registered {} processor: {}", apmType, processor.getClass().getSimpleName());
                }
                loadedCount++;
            } else {
                log.trace("Skipping APM processor for {} (not present)", apmType);
                skippedCount++;
            }
        }

        if (loadedCount > 0) {
            log.info("Loaded {} APM processors (skipped {})", loadedCount, skippedCount);
        }
    }

    /**
     * 注册 APM 上下文处理器
     *
     * @param processor 上下文处理器
     * @return 之前注册的处理器，如果没有则返回 null
     */
    public APMContextProcessor register(APMContextProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }

        APMType apmType = processor.supportedAPM();
        APMContextProcessor previous = processors.put(apmType, processor);
        if (previous == null) {
            log.debug("Registered APM processor for: {}", apmType);
        } else {
            log.debug("Replaced APM processor for: {} (was {})", apmType, previous.getClass().getSimpleName());
        }
        return previous;
    }

    /**
     * 获取指定 APM 类型的上下文处理器
     *
     * @param apmType APM 类型
     * @return 上下文处理器，如果未注册则返回 null
     */
    public APMContextProcessor getProcessor(APMType apmType) {
        return processors.get(apmType);
    }

    /**
     * 获取当前检测到的 APM 对应的上下文处理器
     *
     * @return 上下文处理器，如果未检测到 APM 或未注册则返回 null
     */
    public APMContextProcessor getProcessorForCurrentAPM() {
        if (DETECTED_APM == APMType.NONE) {
            return null;
        }

        APMContextProcessor processor = processors.get(DETECTED_APM);
        if (processor == null) {
            log.warn("APM {} detected but no processor registered", DETECTED_APM);
        }
        return processor;
    }

    /**
     * 获取所有已注册的处理器
     *
     * @return 处理器列表
     */
    public List<APMContextProcessor> getAllProcessors() {
        return new ArrayList<>(processors.values());
    }

    /**
     * 注销指定 APM 类型的处理器
     *
     * @param apmType APM 类型
     * @return 被注销的处理器，如果没有则返回 null
     */
    public APMContextProcessor unregister(APMType apmType) {
        APMContextProcessor removed = processors.remove(apmType);
        if (removed != null) {
            log.debug("Unregistered APM processor for: {}", apmType);
        }
        return removed;
    }

    /**
     * 清空所有已注册的处理器
     */
    public void clear() {
        processors.clear();
        log.debug("Cleared all APM processors");
    }
}