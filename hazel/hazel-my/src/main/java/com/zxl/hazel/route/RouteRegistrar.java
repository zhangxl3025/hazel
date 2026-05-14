package com.zxl.hazel.route;

import com.zxl.hazel.annotation.*;
import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.util.RouteMatcher;
import com.zxl.hazel.web.DispatcherServlet;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 路由注册器
 */
public class RouteRegistrar {

    private static final Logger log = LoggerFactory.getLogger(RouteRegistrar.class);
    private static final List<RouteInfo> ROUTES = new ArrayList<>();
    @Setter
    private static DispatcherServlet dispatcherServlet;

    /**
     * 获取所有路由信息
     */
    public static List<RouteInfo> getRoutes() {
        return new ArrayList<>(ROUTES);
    }

    /**
     * 注册所有路由
     */
    public static void registerRoutes() {
        ROUTES.clear();
        for (Class<?> clazz : BeanContainer.getAllClasses()) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                Object controller = BeanContainer.getBean(clazz);
                registerControllerRoutes(controller, clazz);
            }
        }

        if (dispatcherServlet != null) {
            dispatcherServlet.setRoutes(ROUTES);
        }

        log.info("Registered {} routes", ROUTES.size());
    }

    private static void registerControllerRoutes(Object controller, Class<?> clazz) {
        String basePath = "";
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            basePath = clazz.getAnnotation(RequestMapping.class).value();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            String path = null;
            String httpMethod = null;

            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = method.getAnnotation(RequestMapping.class);
                path = rm.value();
                httpMethod = rm.method().toUpperCase();
                if (httpMethod.isEmpty()) httpMethod = "GET";
            } else if (method.isAnnotationPresent(GetMapping.class)) {
                path = method.getAnnotation(GetMapping.class).value();
                httpMethod = "GET";
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                path = method.getAnnotation(PostMapping.class).value();
                httpMethod = "POST";
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                path = method.getAnnotation(PutMapping.class).value();
                httpMethod = "PUT";
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                path = method.getAnnotation(DeleteMapping.class).value();
                httpMethod = "DELETE";
            }

            if (path != null) {
                String fullPath = RouteMatcher.buildFullPath(basePath, path);
                List<String> pathVariables = extractPathVariables(fullPath);
                Pattern pattern = RouteMatcher.compilePattern(fullPath);
                List<String> parameterNames = extractParameterNames(method);
                List<Class<?>> parameterTypes = extractParameterTypes(method);

                RouteInfo routeInfo = new RouteInfo();
                routeInfo.controller = controller;
                routeInfo.method = method;
                routeInfo.pathPattern = fullPath;
                routeInfo.httpMethod = httpMethod;
                routeInfo.pathVariables = pathVariables;
                routeInfo.pattern = pattern;
                routeInfo.parameterNames = parameterNames;
                routeInfo.parameterTypes = parameterTypes;

                ROUTES.add(routeInfo);
                log.info("Found route: {} {} -> {}.{} (params: {})",
                        httpMethod, fullPath,
                        clazz.getSimpleName(), method.getName(),
                        parameterTypes.size());
            }
        }
    }



    private static List<String> extractPathVariables(String pathPattern) {
        List<String> variables = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{(\\w+)}");
        java.util.regex.Matcher matcher = pattern.matcher(pathPattern);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }



    private static List<String> extractParameterNames(Method method) {
        List<String> names = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            names.add(parameter.getName());
        }
        return names;
    }

    private static List<Class<?>> extractParameterTypes(Method method) {
        List<Class<?>> types = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            types.add(parameter.getType());
        }
        return types;
    }
}