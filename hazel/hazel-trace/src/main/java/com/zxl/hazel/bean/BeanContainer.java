package com.zxl.hazel.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hazel 轻量级容器 - Bean 管理
 */
public class BeanContainer {

    private static final Logger log = LoggerFactory.getLogger(BeanContainer.class);

    // Bean 存储
    private final static Map<Class<?>, Object> beans = new ConcurrentHashMap<>();
    private final static Map<String, Object> namedBeans = new ConcurrentHashMap<>();

    private BeanContainer() {}


    // ==================== Bean 管理 ====================

    public static  <T> void register(Class<T> clazz, Object instance) {
        beans.put(clazz, instance);
        log.debug("Registered bean: {}", clazz.getName());
    }

    public static void register(String name, Object instance) {
        namedBeans.put(name, instance);
        log.debug("Registered named bean: {}", name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz) {
        return (T) beans.get(clazz);
    }

    public static Object getBean(String name) {
        return namedBeans.get(name);
    }

    public static boolean contains(Class<?> clazz) {
        return beans.containsKey(clazz);
    }

    public static Iterable<Class<?>> getAllClasses() {
        return new ArrayList<>(beans.keySet());
    }

    public static void clear() {
        beans.clear();
        namedBeans.clear();
        log.debug("Container cleared");
    }

    public static int size() {
        return beans.size();
    }
}