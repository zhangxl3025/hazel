package com.zxl.hazel.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;

public class JsonUtils {

    /**
     * -- GETTER --
     *  获取 ObjectMapper 实例（用于高级配置）
     */
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // 配置格式化输出
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // 忽略未知属性
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
    
    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
    
    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> targetType) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

}