package com.zxl.hazel.web;

import com.zxl.hazel.route.RouteInfo;
import com.zxl.hazel.route.RouteRegistrar;
import com.zxl.hazel.util.ArgumentResolver;
import com.zxl.hazel.util.JsonUtils;
import com.zxl.hazel.util.RouteMatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class DispatcherServlet extends HttpServlet {

    private List<RouteInfo> routes = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        // 注意：这里不再调用 registerRoutes，因为路由已经在 TomcatWebServer.start() 中注册过了
        // 但为了兼容 Netty 或其他场景，如果 routes 为空则注册
        if (routes.isEmpty()) {
            RouteRegistrar.setDispatcherServlet(this);
            RouteRegistrar.registerRoutes();
        }
        log.info("DispatcherServlet initialized with {} routes", routes.size());
    }

    public void setRoutes(List<RouteInfo> routes) {
        this.routes = new ArrayList<>(routes);
        log.info("Routes set: {}", this.routes.size());
    }

    public void clearRoutes() {
        this.routes.clear();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String method = req.getMethod();
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        RouteInfo route = RouteMatcher.findRoute(routes, method, uri);
        if (route == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"No handler found for " + method + " " + uri + "\"}");
            return;
        }

        try {
            Map<String, String> pathParams = RouteMatcher.extractPathParams(route, uri);
            // ✅ 修复：处理 null 的情况
            String queryString = req.getQueryString();
            Map<String, String> queryParams = RouteMatcher.parseQueryParams(queryString);
            Object[] args = ArgumentResolver.prepareArguments(route.method, req, resp, pathParams, queryParams);
            log.info("Method: {}, Args length: {}", route.method.getName(), args.length);
            for (int i = 0; i < args.length; i++) {
                log.info("Arg[{}]: {}", i, args[i]);
            }
            // 处理返回值
            Object result = route.method.invoke(route.controller, args);
// 处理返回值
            if (result != null && !resp.isCommitted()) {
                resp.setContentType("application/json;charset=UTF-8");
                String jsonResult = JsonUtils.toJson(result);
                resp.getWriter().write(jsonResult);
            }
        } catch (Exception e) {
            log.error("Handler error", e);
            resp.setStatus(500);
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            resp.getWriter().write("{\"error\":\"" + (msg != null ? msg.replace("\"", "\\\"") : "Unknown") + "\"}");
        }
    }
}