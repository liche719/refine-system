package com.achobeta.refine.identity.account.application.port;

public interface TokenPort {
    TokenPair issue(String userId);
    RefreshTokens refresh(String refreshToken);
    void invalidate(String refreshToken);

    record TokenPair(String accessToken, String refreshToken) { }
    record RefreshTokens(String newAccessToken, String newRefreshToken) { }
}
