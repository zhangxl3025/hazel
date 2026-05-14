package com.zxl.hazel.plugin;

import com.zxl.hazel.properties.PropertiesConfig;
import com.zxl.hazel.event.Event;
import com.zxl.hazel.web.netty.NettyWebServer;
import com.zxl.hazel.web.TomcatWebServer;
import com.zxl.hazel.web.WebServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web 服务器插件 - 根据配置选择 Tomcat 或 Netty
 */
public class WebPlugin implements Plugin {

    private static final Logger log = LoggerFactory.getLogger(WebPlugin.class);
    private WebServer webServer;

    @Override
    public String name() {
        return "web";
    }


    @Override
    public Event doStart() {
        int port = PropertiesConfig.getInt("hazel.plugin.web.port", 8080);
        String serverType = PropertiesConfig.get("hazel.plugin.web.type", "tomcat");

        log.info("Starting WebPlugin with type: {}, port: {}", serverType, port);

        if ("netty".equalsIgnoreCase(serverType)) {
            webServer = new NettyWebServer();
        } else {
            webServer = new TomcatWebServer();
        }

        webServer.start( port);

        return PluginEvent.WEB_STARTED;
    }

    @Override
    public void stop() {
        if (webServer != null) {
            webServer.stop();
        }
    }


}