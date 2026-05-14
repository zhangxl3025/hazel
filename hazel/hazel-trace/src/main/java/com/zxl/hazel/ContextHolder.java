package com.zxl.hazel;

/**
 * SPI接口：跨线程上下文传递扩展点（泛型版本）
 * 由各个上下文实现类自行实现捕获、恢复、清理逻辑
 * 
 * @param <T> 上下文类型
 * @author hazel
 */
public interface ContextHolder<T> {
    
    /**
     * 捕获当前线程的上下文
     * 
     * @return 上下文快照对象，如果无上下文则返回null
     */
    T capture();
    
    /**
     * 将捕获的上下文恢复到当前线程
     * 
     * @param snapshot 上下文快照，可能为null（表示主线程无上下文）
     */
    void restore(T snapshot);
    
    /**
     * 清理当前线程的上下文（只清理当前线程，不删除快照）
     * 注意：不是删除快照对象，是清理当前线程中被恢复的ThreadLocal
     * 快照对象由GC回收
     */
    void cleanup();
}
