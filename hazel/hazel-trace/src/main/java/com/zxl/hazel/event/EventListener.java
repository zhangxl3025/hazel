package com.zxl.hazel.event;

import java.lang.annotation.*;

/**
 * 事件监听注解
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
}