package com.zxl.hazel.plugin;


import com.zxl.hazel.event.Event;
import com.zxl.hazel.event.EventTransfer;

/**
 * 插件接口
 */
public interface Plugin {

    /**
     * 插件名称
     */
    String name();


    /**
     * 启动顺序（越小越先启动）
     * <p>
     * 用户自定义插件取值范围：1~97，其余为框架保留区。
     */
    default int order() {
        return 50;
    }

    /**
     * 是否启用（从配置读取）
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 启动插件
     */
    default void start() {
        // 执行具体启动逻辑
        Event event = doStart();

        // 发布启动后事件
        EventTransfer.publish(event);
    }


    Event doStart();


    /**
     * 停止插件
     */
    default void stop() {
        // 默认空实现
    }
}