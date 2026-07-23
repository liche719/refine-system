package com.achobeta.refine.identity.account.application.model;

public final class AccountResults {
    private AccountResults() {
    }

    public record Login(String userId, String userName, String accessToken, String refreshToken) { }
    public record Refresh(String newAccessToken, String newRefreshToken) { }
}
