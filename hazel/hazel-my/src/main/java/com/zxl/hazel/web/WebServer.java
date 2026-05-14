package com.zxl.hazel.web;

/**
 * Web 服务器抽象接口
 */
public interface WebServer {
    
    /**
     * 启动服务器
     */
    void start(int port);
    
    /**
     * 停止服务器
     */
    void stop();
    
    /**
     * 注册路由（由 RouteRegistrar 调用）
     */
    void registerRoutes();
    
    /**
     * 获取服务器状态
     */
    boolean isRunning();
    
    /**
     * 获取端口
     */
    int getPort();
}