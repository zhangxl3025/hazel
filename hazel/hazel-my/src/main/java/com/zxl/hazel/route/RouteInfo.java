package com.zxl.hazel.route;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 路由信息类
 */
@Getter
public class RouteInfo {
    public Object controller;
    public Method method;
    public String pathPattern;
    public String httpMethod;
    public List<String> pathVariables;
    public Pattern pattern;
    public List<String> parameterNames;
    public List<Class<?>> parameterTypes;

}