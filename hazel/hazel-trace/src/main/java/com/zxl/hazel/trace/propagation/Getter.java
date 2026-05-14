package com.zxl.hazel.trace.propagation;

/**
 * 载体获取接口
 */
public interface Getter<C> {
    String get(C carrier, String key);
}
