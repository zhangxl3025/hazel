package com.zxl.hazel.demo.base;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HazelResponse<T> {

    private String code;
    private T data;
    private String error;

    public static <T> HazelResponse<T> ok(T data) {
        return new HazelResponse<>("ok", data, null);
    }

    public static <T> HazelResponse<T> fail(String error) {
        return new HazelResponse<>("fail", null, error);
    }
}
