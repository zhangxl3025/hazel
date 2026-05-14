package com.zxl.hazel.event;

import java.lang.reflect.Method;

/**
 * 事件监听器包装器
 */
class EventListenerWrapper {
    private final Object target;
    private final Method method;
    private final EventCallback callback;

    // 方法监听器
    public EventListenerWrapper(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.callback = null;
    }

    // 回调监听器
    public EventListenerWrapper(EventCallback callback) {
        this.target = null;
        this.method = null;
        this.callback = callback;
    }

    public void invoke(Event event) throws Exception {
        if (method != null) {
            method.invoke(target, event);
        } else if (callback != null) {
            callback.onEvent(event);
        }
    }
}
