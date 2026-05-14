package com.zxl.hazel.util;

import com.zxl.hazel.annotation.PathVariable;
import com.zxl.hazel.annotation.RequestBody;
import com.zxl.hazel.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class ArgumentResolver {

    // Servlet 版本
    public static Object[] prepareArguments(Method method,
                                            HttpServletRequest req,
                                            HttpServletResponse resp,
                                            Map<String, String> pathParams,
                                            Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (paramType == HttpServletRequest.class) {
                args[i] = req;
            } else if (paramType == HttpServletResponse.class) {
                args[i] = resp;
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = param.getAnnotation(PathVariable.class);
                String value = pathParams.get(pv.value());
                args[i] = convertValue(value, paramType);
            } else if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = param.getAnnotation(RequestParam.class);
                String value = queryParams.get(rp.value());
                if (value == null) {
                    if (rp.required() && rp.defaultValue().isEmpty()) {
                        throw new IllegalArgumentException("Required parameter '" + rp.value() + "' is missing");
                    }
                    value = rp.defaultValue();
                }
                args[i] = convertValue(value, paramType);
            } else if (param.isAnnotationPresent(RequestBody.class)) {
                args[i] = readBody(req);
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    // Netty 版本
    public static Object[] prepareArgumentsForNetty(Method method,
                                                    Map<String, String> pathParams,
                                                    Map<String, String> queryParams,
                                                    String body) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // Netty 环境下不处理 Servlet 类型
            if (paramType == HttpServletRequest.class || paramType == HttpServletResponse.class) {
                args[i] = null;
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = param.getAnnotation(PathVariable.class);
                String value = pathParams.get(pv.value());
                args[i] = convertValue(value, paramType);
            } else if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = param.getAnnotation(RequestParam.class);
                String value = queryParams.get(rp.value());
                if (value == null) {
                    if (rp.required() && rp.defaultValue().isEmpty()) {
                        throw new IllegalArgumentException("Required parameter '" + rp.value() + "' is missing");
                    }
                    value = rp.defaultValue();
                }
                args[i] = convertValue(value, paramType);
            } else if (param.isAnnotationPresent(RequestBody.class)) {
                args[i] = parseBody(body, paramType);
            } else if (paramType == String.class) {
                args[i] = body;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private static Object parseBody(String body, Class<?> targetType) {
        if (targetType == String.class) return body;
        // 使用 JsonUtils 反序列化
        return JsonUtils.fromJson(body, targetType);
    }

    private static String readBody(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        try {
            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert '" + value + "' to " + targetType.getSimpleName(), e);
        }
        return value;
    }
}