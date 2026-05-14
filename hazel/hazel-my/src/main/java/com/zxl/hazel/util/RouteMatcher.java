package com.zxl.hazel.util;

import com.zxl.hazel.route.RouteInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由匹配工具类
 * 抽取 DispatcherServlet 和 NettyDispatcherHandler 的公共逻辑
 */
public class RouteMatcher {





    /**
     * 查找匹配的路由
     */
    public static RouteInfo findRoute(List<RouteInfo> routes, String method, String path) {
        for (RouteInfo route : routes) {
            if (route.httpMethod.equals(method)) {
                Matcher matcher = route.pattern.matcher(path);
                if (matcher.matches()) {
                    return route;
                }
            }
        }
        return null;
    }
    
    /**
     * 提取路径参数
     */
    public static Map<String, String> extractPathParams(RouteInfo route, String path) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = route.pattern.matcher(path);
        if (matcher.matches()) {
            for (int i = 0; i < route.pathVariables.size(); i++) {
                params.put(route.pathVariables.get(i), matcher.group(i + 1));
            }
        }
        return params;
    }
    
    /**
     * 解析查询参数
     */
    public static Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> params = new java.util.HashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return params;
    }
    
    /**
     * 构建完整路径
     */
    public static String buildFullPath(String basePath, String methodPath) {
        if (basePath == null || basePath.isEmpty()) return methodPath;
        if (methodPath == null || methodPath.isEmpty()) return basePath;
        if (basePath.endsWith("/") && methodPath.startsWith("/")) {
            return basePath + methodPath.substring(1);
        }
        if (!basePath.endsWith("/") && !methodPath.startsWith("/")) {
            return basePath + "/" + methodPath;
        }
        return basePath + methodPath;
    }
    
    /**
     * 提取路径变量名
     */
    public static List<String> extractPathVariables(String pathPattern) {
        List<String> variables = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(pathPattern);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }
    
    /**
     * 编译路径为正则表达式
     */
    public static Pattern compilePattern(String pathPattern) {
        String regex = pathPattern.replaceAll("\\{\\w+\\}", "([^/]+)");
        regex = "^" + regex + "$";
        return Pattern.compile(regex);
    }
}