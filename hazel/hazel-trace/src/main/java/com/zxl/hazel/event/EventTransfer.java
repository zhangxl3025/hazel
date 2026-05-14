package com.zxl.hazel.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件传输器 - 事件发布订阅
 */
public class EventTransfer {

    private static final Logger log = LoggerFactory.getLogger(EventTransfer.class);

    // 事件监听器存储（用 source 字符串作为 Key）
    private static final ConcurrentHashMap<String, List<EventListenerWrapper>> eventListeners = new ConcurrentHashMap<>();

    private EventTransfer() {
    }


    // ==================== 事件发布 ====================

    /**
     * 发布事件
     */
    public static void publish(Event event) {
        if (event == null) return;

        String source = event.source();
        List<EventListenerWrapper> listeners = eventListeners.get(source);

        if (listeners != null && !listeners.isEmpty()) {
            for (EventListenerWrapper wrapper : listeners) {
                try {
                    wrapper.invoke(event);
                } catch (Exception e) {
                    log.error("Failed to invoke event listener for: {}", source, e);
                }
            }
        }

        log.debug("Event published: {}", source);
    }

    // ==================== 事件订阅 ====================

    /**
     * 订阅事件（按 source 字符串）
     */
    public static void subscribe(String source, EventCallback callback) {
        eventListeners.computeIfAbsent(source, k -> new CopyOnWriteArrayList<>())
                .add(new EventListenerWrapper(callback));
        log.debug("Subscribed to event: {}", source);
    }

    /**
     * 订阅事件（按事件类型）
     */
    public static void subscribe(Class<? extends Event> eventClass, EventCallback callback) {
        try {
            Event instance = eventClass.getDeclaredConstructor().newInstance();
            subscribe(instance.source(), callback);
        } catch (Exception e) {
            log.error("Failed to create event instance for: {}", eventClass, e);
        }
    }

    // ==================== 监听器注册 ====================

    /**
     * 注册方法监听器
     */
    public static void registerListener(Object target, Method method, String source) {
        eventListeners.computeIfAbsent(source, k -> new CopyOnWriteArrayList<>())
                .add(new EventListenerWrapper(target, method));
        log.debug("Registered listener: {}.{} -> {}",
                target.getClass().getSimpleName(), method.getName(), source);
    }

    /**
     * 扫描并注册所有监听器方法（方法名以 on 开头）
     */
    /**
     * 扫描并注册所有监听器方法（使用 @EventListener 注解）
     */
    public static void registerListenerMethods(Object bean) {
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            // 检查是否有 @EventListener 注解
            if (method.isAnnotationPresent(EventListener.class)) {
                Class<?>[] paramTypes = method.getParameterTypes();

                // 必须有且只有一个参数，且参数类型是 Event 的子类
                if (paramTypes.length == 1 && Event.class.isAssignableFrom(paramTypes[0])) {
                    method.setAccessible(true);
                    Class<?> eventType = paramTypes[0];

                    try {
                        Event eventInstance = (Event) eventType.getDeclaredConstructor().newInstance();
                        registerListener(bean, method, eventInstance.source());
                        log.debug("Registered event listener: {}.{} -> {}",
                                bean.getClass().getSimpleName(), method.getName(), eventInstance.source());
                    } catch (Exception e) {
                        log.error("Failed to register listener: {}.{}",
                                bean.getClass().getSimpleName(), method.getName(), e);
                    }
                } else {
                    log.warn("@EventListener method must have exactly one parameter of type Event: {}.{}",
                            bean.getClass().getSimpleName(), method.getName());
                }
            }
        }
    }


    public static void clear() {
        eventListeners.clear();
        log.debug("EventTransfer cleared");
    }


}