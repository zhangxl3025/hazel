package com.zxl.hazel.web;

import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.route.RouteRegistrar;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TomcatWebServer implements WebServer {

    private static final Logger log = LoggerFactory.getLogger(TomcatWebServer.class);
    private Tomcat tomcat;
    private DispatcherServlet dispatcherServlet;
    private BeanContainer container;
    private int port;
    private boolean running;

    @Override
    public void start(int port) {
        this.port = port;

        try {
            // 先创建 DispatcherServlet 并注册路由
            dispatcherServlet = new DispatcherServlet();
            RouteRegistrar.setDispatcherServlet(dispatcherServlet);
            RouteRegistrar.registerRoutes();

            tomcat = new Tomcat();
            tomcat.setPort(port);

            File baseDir = new File(System.getProperty("java.io.tmpdir"), "hazel-tomcat");
            tomcat.setBaseDir(baseDir.getAbsolutePath());

            File docBase = new File(baseDir, "webapp");
            docBase.mkdirs();
            Context ctx = tomcat.addContext("", docBase.getAbsolutePath());

            // 注册 DispatcherServlet
            Tomcat.addServlet(ctx, "dispatcherServlet", dispatcherServlet);
            ctx.addServletMappingDecoded("/*", "dispatcherServlet");

            // 设置容器到 ServletContext，供 DispatcherServlet.init() 使用
            ctx.getServletContext().setAttribute("hazelContainer", container);

            tomcat.getConnector();
            tomcat.start();

            running = true;
            log.info("Tomcat WebServer started on port {}", port);

            // 保持运行
            new Thread(() -> tomcat.getServer().await()).start();

        } catch (LifecycleException e) {
            log.error("Failed to start Tomcat", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerRoutes() {
        // 路由已经在 start() 中注册过了，这里不需要重复注册
        // 保留方法只是为了实现接口
    }

    @Override
    public void stop() {
        if (tomcat != null) {
            try {
                tomcat.stop();
                tomcat.destroy();
                running = false;
                log.info("Tomcat WebServer stopped");
            } catch (LifecycleException e) {
                log.error("Failed to stop Tomcat", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPort() {
        return port;
    }
}