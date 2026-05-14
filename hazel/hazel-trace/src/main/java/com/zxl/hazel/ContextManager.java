package com.zxl.hazel;

import com.zxl.hazel.sync.SyncContext;
import com.zxl.hazel.sync.SyncContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 上下文管理器：基于SPI + 闭包的通用跨线程上下文传递框架
 */
public class ContextManager {

    private static final List<ContextHolder<?>> HOLDERS = new ArrayList<>();

    static {
        ServiceLoader.load(ContextHolder.class).forEach(HOLDERS::add);
    }

    private static List<ContextHolder<?>> getHolders() {
        return HOLDERS;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ContextHolder<?>> T get(Class<T> type) {
        for (ContextHolder<?> holder : HOLDERS) {
            if (type.isInstance(holder)) {
                return (T) holder;
            }
        }
        return null;
    }

    private static class CaptureHolder {
        private final ContextHolder<?> spi;
        private final Object snapshot;

        CaptureHolder(ContextHolder<?> spi, Object snapshot) {
            this.spi = spi;
            this.snapshot = snapshot;
        }

        @SuppressWarnings("unchecked")
        void restore() {
            ((ContextHolder<Object>) spi).restore(snapshot);
        }

        void cleanup() {
            spi.cleanup();
        }
    }

    private static class CaptureContext {
        private final CaptureHolder[] holders;

        CaptureContext(CaptureHolder[] holders) {
            this.holders = holders;
        }
    }

    // ==================== wrap 系列 ====================

    public static Runnable wrap(Runnable task) {
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                task.run();
            } finally {
                cleanupContext(context);
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> task) {
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                return task.call();
            } finally {
                cleanupContext(context);
            }
        };
    }

    public static <T> Supplier<T> wrap(Supplier<T> task) {
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                return task.get();
            } finally {
                cleanupContext(context);
            }
        };
    }

    // ==================== wrapWithCount 系列 ====================

    public static Runnable wrapWithCount(Runnable task) {
        SyncContext txContext = SyncContextHolder.getInstance().capture();        Thread thread = Thread.currentThread();
        if (txContext != null) {
            txContext.incrementRefCount(thread);
        }
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                task.run();
            } catch (Throwable t) {
                if (txContext != null) {
                    txContext.recordException(thread, t);
                }
                throw t;
            } finally {
                if (txContext != null) {
                    txContext.decrementRefCount(thread);
                }
                cleanupContext(context);
            }
        };
    }

    public static <T> Callable<T> wrapWithCount(Callable<T> task) {
        SyncContext txContext = SyncContextHolder.getInstance().capture();
        Thread thread = Thread.currentThread();
        if (txContext != null) {
            txContext.incrementRefCount(thread);
        }
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                return task.call();
            } catch (Throwable t) {
                if (txContext != null) {
                    txContext.recordException(thread, t);
                }
                throw t;
            } finally {
                if (txContext != null) {
                    txContext.decrementRefCount(thread);
                }
                cleanupContext(context);
            }
        };
    }

    public static <T> Supplier<T> wrapWithCount(Supplier<T> task) {
        SyncContext txContext = SyncContextHolder.getInstance().capture();        Thread thread = Thread.currentThread();
        if (txContext != null) {
            txContext.incrementRefCount(thread);
        }
        CaptureContext context = captureContext();
        return () -> {
            try {
                restoreContext(context);
                return task.get();
            } catch (Throwable t) {
                if (txContext != null) {
                    txContext.recordException(thread, t);
                }
                throw t;
            } finally {
                if (txContext != null) {
                    txContext.decrementRefCount(thread);
                }
                cleanupContext(context);
            }
        };
    }

    // ==================== 私有方法 ====================

    private static CaptureContext captureContext() {
        List<ContextHolder<?>> currentHolders = getHolders();
        CaptureHolder[] holders = new CaptureHolder[currentHolders.size()];
        for (int i = 0; i < currentHolders.size(); i++) {
            holders[i] = new CaptureHolder(currentHolders.get(i), currentHolders.get(i).capture());
        }
        return new CaptureContext(holders);
    }

    private static void restoreContext(CaptureContext context) {
        for (CaptureHolder holder : context.holders) {
            holder.restore();
        }
    }

    private static void cleanupContext(CaptureContext context) {
        for (CaptureHolder holder : context.holders) {
            holder.cleanup();
        }
    }
}