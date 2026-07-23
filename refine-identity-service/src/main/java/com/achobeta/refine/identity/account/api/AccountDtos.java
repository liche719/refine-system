package com.achobeta.refine.identity.account.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AccountDtos {
    private static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    private AccountDtos() {
    }

    public record LoginRequest(@NotBlank String userAccount, @NotBlank String userPassword) {
    }

    public record RegisterRequest(
            @NotBlank @Email String userAccount,
            @NotBlank @Pattern(regexp = PASSWORD) String userPassword,
            @NotBlank String userName,
            @NotBlank String checkCode) {
    }

    public record LoginResponse(String userId, String userName, String accessToken, String refreshToken) {
    }

    public record RefreshResponse(String newAccessToken, String newRefreshToken) {
    }
}
