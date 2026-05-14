package com.zxl.hazel.apm;

/**
 * APM 上下文处理器 SPI 接口
 * 
 * 用于将不同 APM 的上下文传递能力集成到 Hazel 的 ContextManager 中。
 * 当 APM 无法覆盖某些场景（如自定义线程池、断链等）时，由 Hazel 接管上下文传递。
 * 
 * <p>实现示例：
 * <pre>
 * // SkyWalking 上下文处理器
 * public class SkyWalkingContextProcessor implements APMContextProcessor {
 *     public String capture() {
 *         return TraceContextManager.INSTANCE.get();
 *     }
 *     
 *     public void restore(String snapshot) {
 *         TraceContextManager.INSTANCE.set(snapshot);
 *     }
 *     
 *     public void cleanup() {
 *         TraceContextManager.INSTANCE.remove();
 *     }
 * }
 * </pre>
 * 
 * @author hazel
 */
public interface APMContextProcessor {
    
    /**
     * 获取支持的 APM 类型
     * 
     * @return APM 类型
     */
    APMType supportedAPM();
    
    /**
     * 捕获当前线程的 APM 上下文
     * 
     * @return 上下文快照，如果无上下文则返回 null
     */
    Object capture();
    
    /**
     * 将捕获的上下文恢复到当前线程
     * 
     * @param snapshot 上下文快照，可能为 null
     */
    void restore(Object snapshot);
    
    /**
     * 清理当前线程的 APM 上下文
     * 注意：只清理当前线程，不删除快照
     */
    void cleanup();
    
    /**
     * 判断当前 APM 是否可用
     * 
     * @return true 如果该 APM 已接入且可用
     */
    default boolean isAvailable() {
        return supportedAPM().isPresent();
    }
}
