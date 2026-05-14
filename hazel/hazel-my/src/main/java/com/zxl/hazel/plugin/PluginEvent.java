package com.zxl.hazel.plugin;

import com.zxl.hazel.event.Event;
import lombok.Getter;

/**
 * 插件事件
 */
@Getter
public enum PluginEvent implements Event {

    // 格式：插件名_事件类型
    JDBC_STARTED("jdbc", EventType.STARTED),
    JDBC_STOPPED("jdbc", EventType.STOPPED),
    JDBC_FAILED("jdbc", EventType.FAILED),

    WEB_STARTED("web", EventType.STARTED),
    WEB_STOPPED("web", EventType.STOPPED),
    WEB_FAILED("web", EventType.FAILED),

    REDIS_STARTED("redis", EventType.STARTED),
    REDIS_STOPPED("redis", EventType.STOPPED),

    CONTAINER_READY("container", EventType.READY),
    CONFIG_LOADED("config", EventType.LOADED),
    ROUTES_REGISTERED("route", EventType.REGISTERED);

    private final String pluginName;
    private final EventType eventType;

    PluginEvent(String pluginName, EventType eventType) {
        this.pluginName = pluginName;
        this.eventType = eventType;
    }

    /**
     * 动态生成插件事件（给第三方插件用）
     */
    public static PluginEvent of(String pluginName, EventType eventType) {
        // 先查找是否有预定义
        for (PluginEvent event : values()) {
            if (event.pluginName.equals(pluginName) && event.eventType == eventType) {
                return event;
            }
        }
        // 没有就新建（需要配合字符串解析）
        throw new IllegalArgumentException("Event not predefined: " + pluginName + "_" + eventType);
    }


    public String source() {
        return name();
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        STARTED,
        STOPPED,
        FAILED,
        READY,
        LOADED,
        REGISTERED
    }
}