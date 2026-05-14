package com.zxl.hazel.trace.propagation;

/**
 * 载体设置接口
 */
public interface Setter<C> {
    void set(C carrier, String key, String value);
}
