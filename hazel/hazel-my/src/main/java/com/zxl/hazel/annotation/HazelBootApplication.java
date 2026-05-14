package com.zxl.hazel.annotation;

import java.lang.annotation.*;

/**
 * Hazel Boot 启动注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HazelBootApplication {
    
    /**
     * 扫描包路径（默认启动类所在包）
     */
    String[] scanPackages() default {};
    
    /**
     * 排除的类
     */
    Class<?>[] exclude() default {};
}