package com.achobeta.refine.identity.account.infrastructure.security;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.port.TokenPort;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtRedisTokenAdapter implements TokenPort {
    private static final String REFRESH_PREFIX = "identity:refresh-token:";
    private final StringRedisTemplate redis;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtRedisTokenAdapter(StringRedisTemplate redis, @Value("${refine.security.jwt-secret}") String secret,
                                @Value("${refine.security.access-token-ttl:PT30M}") Duration accessTtl,
                                @Value("${refine.security.refresh-token-ttl:P2D}") Duration refreshTtl) {
        this.redis = redis; this.algorithm = Algorithm.HMAC256(secret); this.verifier = JWT.require(algorithm).build();
        this.accessTtl = accessTtl; this.refreshTtl = refreshTtl;
    }

    @Override
    public TokenPair issue(String userId) {
        Instant now = Instant.now();
        String access = accessToken(userId, now);
        String jti = UUID.randomUUID().toString();
        String refresh = JWT.create().withClaim("userId", userId).withClaim("type", "refresh").withJWTId(jti)
                .withIssuedAt(Date.from(now)).withExpiresAt(Date.from(now.plus(refreshTtl))).sign(algorithm);
        redis.opsForValue().set(REFRESH_PREFIX + jti, userId, refreshTtl);
        return new TokenPair(access, refresh);
    }

    @Override
    public RefreshTokens refresh(String refreshToken) {
        DecodedJWT decoded = verifyRefresh(refreshToken);
        return new RefreshTokens(accessToken(decoded.getClaim("userId").asString(), Instant.now()), refreshToken);
    }

    @Override
    public void invalidate(String refreshToken) {
        try {
            DecodedJWT decoded = verifier.verify(refreshToken);
            if (decoded.getId() != null) redis.delete(REFRESH_PREFIX + decoded.getId());
        } catch (Exception ignored) {
            // Logout is intentionally idempotent.
        }
    }

    private String accessToken(String userId, Instant now) {
        return JWT.create().withClaim("userId", userId).withClaim("type", "access")
                .withIssuedAt(Date.from(now)).withExpiresAt(Date.from(now.plus(accessTtl))).sign(algorithm);
    }

    private DecodedJWT verifyRefresh(String refreshToken) {
        try {
            DecodedJWT decoded = verifier.verify(refreshToken);
            String userId = decoded.getClaim("userId").asString();
            String activeUser = decoded.getId() == null ? null : redis.opsForValue().get(REFRESH_PREFIX + decoded.getId());
            if (!"refresh".equals(decoded.getClaim("type").asString()) || userId == null || !userId.equals(activeUser)) {
                throw new AppException(401, "refresh-token is no longer active");
            }
            return decoded;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(401, "refresh-token is invalid or expired");
        }
    }
}
