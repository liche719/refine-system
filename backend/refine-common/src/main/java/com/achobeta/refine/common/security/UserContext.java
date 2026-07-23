package com.achobeta.refine.common.security;

import com.achobeta.refine.common.api.AppException;

public final class UserContext {
    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String userId) {
        USER.set(userId);
    }

    public static String get() {
        String userId = USER.get();
        if (userId == null || userId.isBlank()) {
            throw new AppException(401, "未登录或用户身份缺失");
        }
        return userId;
    }

    public static void clear() {
        USER.remove();
    }
}
