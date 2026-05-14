package com.zxl.hazel.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 插件管理器
 */
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
    private static final List<Plugin> plugins = new ArrayList<>();

    /**
     * 加载所有 SPI 插件
     */
    public static void loadPlugins() {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        for (Plugin plugin : loader) {
            if (plugin.isEnabled()) {
                plugins.add(plugin);
                log.info("Loaded plugin: {} (order={})", plugin.name(), plugin.order());
            } else {
                log.info("Plugin disabled: {}", plugin.name());
            }
        }

        // 按 order 排序
        plugins.sort(Comparator.comparingInt(Plugin::order));
    }

    /**
     * 启动所有插件
     */
    public static void startPlugins() {
        for (Plugin plugin : plugins) {
            try {
                log.info("Starting plugin: {}", plugin.name());
                plugin.start();
                log.info("Plugin started: {}", plugin.name());
            } catch (Exception e) {
                log.error("Failed to start plugin: {}", plugin.name(), e);
            }
        }
    }

    /**
     * 停止所有插件
     */
    public static void stopPlugins() {
        for (Plugin plugin : plugins) {
            try {
                plugin.stop();
                log.info("Plugin stopped: {}", plugin.name());
            } catch (Exception e) {
                log.error("Failed to stop plugin: {}", plugin.name(), e);
            }
        }
        plugins.clear();
    }

    /**
     * 获取插件
     */
    @SuppressWarnings("unchecked")
    public static <T extends Plugin> T getPlugin(Class<T> pluginClass) {
        for (Plugin plugin : plugins) {
            if (pluginClass.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }
}