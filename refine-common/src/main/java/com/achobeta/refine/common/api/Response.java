package com.achobeta.refine.common.api;

import org.slf4j.MDC;

import java.io.Serializable;

public record Response<T>(String traceId, Integer code, String info, T data) implements Serializable {

    public static <T> Response<T> success() {
        return new Response<>(MDC.get("traceId"), 200, "操作成功", null);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(MDC.get("traceId"), 200, "操作成功", data);
    }

    public static <T> Response<T> error(int code, String info) {
        return new Response<>(MDC.get("traceId"), code, info, null);
    }

    public static <T> Response<T> serviceError(String info) {
        return error(-500, info == null ? "系统异常" : info);
    }
}
