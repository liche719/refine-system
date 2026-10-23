package com.achobeta.refine.identity.account.application.port;

public interface TokenPort {
    TokenPair issue(String userId);

    RefreshTokens refresh(String refreshToken);

    void invalidate(String refreshToken);

    /**
     * 撤销该用户全部 refresh token（改密、重置密码、踢出全部会话时使用）。
     * 已签发的 access token 仍会在剩余有效期内可用，这是无状态 JWT 的固有代价，
     * 因此 access token 有效期应保持较短。
     */
    void revokeAll(String userId);

    record TokenPair(String accessToken, String refreshToken) { }
    record RefreshTokens(String newAccessToken, String newRefreshToken) { }
}
