package com.achobeta.refine.identity.account.domain;

import java.time.LocalDateTime;

public record UserAccount(
        Long id,
        String userId,
        String userName,
        String userAccount,
        String passwordHash,
        Integer status,
        LocalDateTime createdAt) {
    public static UserAccount register(String userId, String name, String email, String passwordHash, LocalDateTime now) {
        if (userId == null || userId.isBlank() || name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("account identity fields must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
        return new UserAccount(null, userId, name.trim(), email.trim(), passwordHash, 1, now);
    }

    public boolean isEnabled() { return status != null && status == 1; }

    public UserAccount changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
        return new UserAccount(id, userId, userName, userAccount, newPasswordHash, status, createdAt);
    }
}
