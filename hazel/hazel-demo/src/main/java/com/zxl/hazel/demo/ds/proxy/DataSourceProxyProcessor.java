package com.zxl.hazel.demo.ds.proxy;

import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class DataSourceProxyProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        // 当 HikariDataSource 初始化完成后，包装成 TraceDataSource
        if (bean instanceof HikariDataSource) {
            return new TraceDataSource((HikariDataSource) bean);
        }
        return bean;
    }
}