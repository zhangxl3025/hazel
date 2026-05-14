package com.zxl.hazel.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Hazel 配置管理
 */
public class PropertiesConfig {
    
    private static final Logger log = LoggerFactory.getLogger(PropertiesConfig.class);
    private static final Properties props = new Properties();
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        // 加载 application.properties
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                log.info("Loaded application.properties");
            }
        } catch (Exception e) {
            log.debug("Failed to load application.properties", e);
        }
    }
    
    // ==================== 获取配置 ====================
    
    public static String get(String key) {
        // 系统属性优先级最高
        String value = System.getProperty(key);
        if (value != null) return value;
        return props.getProperty(key);
    }
    
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
    public static int getInt(String key) {
        String value = get(key);
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static long getLong(String key) {
        String value = get(key);
        try {
            return value != null ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static boolean getBoolean(String key) {
        String value = get(key);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    public static boolean contains(String key) {
        return get(key) != null;
    }
    
    /**
     * 设置配置（用于测试）
     */
    public static void set(String key, String value) {
        props.setProperty(key, value);
    }
    
    /**
     * 清除所有配置
     */
    public static void clear() {
        props.clear();
    }
}