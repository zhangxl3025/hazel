package com.zxl.hazel.sync;

import com.zxl.hazel.ContextHolder;
import com.zxl.hazel.ContextManager;
import com.zxl.hazel.thread.MyThreadLocal;
import com.zxl.hazel.thread.ThreadLocalRegistry;

public class SyncContextHolder implements ContextHolder<SyncContext> {

    public static SyncContextHolder getInstance() {
        return ContextManager.get(SyncContextHolder.class);
    }

    private final MyThreadLocal<SyncContext> myThreadLocal = ThreadLocalRegistry.getInstance(SyncContext::new);

    public SyncContext capture() {
        return myThreadLocal.get();
    }

    @Override
    public void restore(SyncContext snapshot) {
        myThreadLocal.set(snapshot);
    }

    @Override
    public void cleanup() {
        myThreadLocal.remove();
    }

}
