package com.zxl.hazel.sync;

import com.zxl.hazel.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步上下文：跨线程协调（透传，所有线程共享同一个实例）
 * 负责：跨线程计数、等待、收集异常、收集返回结果
 */
public class SyncContext {

    private static final Logger log = LoggerFactory.getLogger(SyncContext.class);

    /**
     * 任务执行结果（合并了异常、返回结果、多结果Map）
     */
    private static class TaskResult {
        private volatile Throwable throwable;
        private volatile Object singleResult;
        private final Map<String, Object> results = new ConcurrentHashMap<>();
        private final AtomicInteger refCount = new AtomicInteger(0);

        void increment() {
            refCount.incrementAndGet();
        }

        void decrement() {
            refCount.decrementAndGet();
        }

        int getCount() {
            return refCount.get();
        }

        void setThrowable(Throwable t) {
            this.throwable = t;
        }

        Throwable getThrowable() {
            return throwable;
        }

        void setSingleResult(Object result) {
            this.singleResult = result;
        }

        Object getSingleResult() {
            return singleResult;
        }

        void putResult(String key, Object value) {
            results.put(key, value);
        }

        Map<String, Object> getResults() {
            return results;
        }

        boolean isSuccess() {
            return throwable == null;
        }

        void clear() {
            results.clear();
            singleResult = null;
            throwable = null;
        }
    }

    private final Map<Thread, TaskResult> threadTasks = new ConcurrentHashMap<>();

    /**
     * 增加引用计数（发起异步任务时调用）
     */
    public void incrementRefCount(Thread thread) {
        threadTasks.computeIfAbsent(thread, k -> new TaskResult()).increment();
    }

    /**
     * 减少引用计数（异步任务完成时调用）
     */
    public void decrementRefCount(Thread thread) {
        TaskResult result = threadTasks.get(thread);
        if (result != null) {
            result.decrement();
            synchronized (threadTasks) {
                threadTasks.notifyAll();
            }
        }
    }

    /**
     * 记录任务异常
     */
    public void recordException(Thread thread, Throwable t) {
        TaskResult result = threadTasks.get(thread);
        if (result != null) {
            result.setThrowable(t);
        }
    }

    /**
     * 记录任务返回结果（带 key）
     */
    public void recordResult(Thread thread, String key, Object value) {
        TaskResult result = threadTasks.get(thread);
        if (result != null) {
            result.putResult(key, value);
        }
    }

    /**
     * 记录任务返回结果（单个值）
     */
    public void recordResult(Thread thread, Object value) {
        TaskResult result = threadTasks.get(thread);
        if (result != null) {
            result.setSingleResult(value);
        }
    }
    public void awaitRefCountZero(Runnable runnable,Thread thread) throws Exception {
        runnable.run();
        awaitRefCountZero(thread);
    }
    /**
     * 等待计数归零，如果任何任务失败则抛出异常
     */
    public void awaitRefCountZero(Thread thread) throws Exception {
        synchronized (threadTasks) {
            TaskResult result = threadTasks.get(thread);
            while (result != null && result.getCount() > 0) {
                threadTasks.wait();
                result = threadTasks.get(thread);
            }

            if (result != null && !result.isSuccess()) {
                Throwable t = result.getThrowable();
                threadTasks.remove(thread);
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else {
                    throw new RuntimeException(t);
                }
            }

            threadTasks.remove(thread);
        }
    }

    /**
     * 等待计数归零，并返回所有结果（带 key 的结果）
     */
    public <T> Map<String, T> awaitWithResults(Thread thread) throws Exception {
        synchronized (threadTasks) {
            TaskResult result = threadTasks.get(thread);
            while (result != null && result.getCount() > 0) {
                threadTasks.wait();
                result = threadTasks.get(thread);
            }

            if (result == null) {
                return new ConcurrentHashMap<>();
            }

            if (!result.isSuccess()) {
                Throwable t = result.getThrowable();
                threadTasks.remove(thread);
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else {
                    throw new RuntimeException(t);
                }
            }

            Map<String, T> res = (Map<String, T>) (Map) result.getResults();
            threadTasks.remove(thread);
            return res == null ? new ConcurrentHashMap<>() : res;
        }
    }

    /**
     * 等待计数归零，并返回单个结果
     */
    @SuppressWarnings("unchecked")
    public <T> T awaitAndGet(Thread thread) throws Exception {
        synchronized (threadTasks) {
            TaskResult result = threadTasks.get(thread);
            while (result != null && result.getCount() > 0) {
                threadTasks.wait();
                result = threadTasks.get(thread);
            }

            if (result == null) {
                return null;
            }

            if (!result.isSuccess()) {
                Throwable t = result.getThrowable();
                threadTasks.remove(thread);
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else {
                    throw new RuntimeException(t);
                }
            }

            T res = (T) result.getSingleResult();
            threadTasks.remove(thread);
            return res;
        }
    }

    public void cleanupThread(Thread thread) {
        TaskResult result = threadTasks.remove(thread);
        if (result != null) {
            result.clear();
        }
    }
}