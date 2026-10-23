package com.achobeta.refine.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "refine.gateway.security")
public record GatewaySecurityProperties(String jwtSecret, String gatewayToken, List<String> publicPaths) {

    /** HMAC-SHA256 的密钥至少要 32 字节，否则等于可离线爆破后伪造任意用户身份。 */
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int MIN_TOKEN_LENGTH = 16;

    public GatewaySecurityProperties {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("${")) {
            throw new IllegalStateException("JWT_SECRET must be configured with a resolved value");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_LENGTH
                    + " characters to be safe with HS256, got " + jwtSecret.length());
        }
        if (gatewayToken == null || gatewayToken.isBlank() || gatewayToken.contains("${")) {
            throw new IllegalStateException("GATEWAY_TOKEN must be configured with a resolved value");
        }
        if (gatewayToken.length() < MIN_TOKEN_LENGTH) {
            throw new IllegalStateException("GATEWAY_TOKEN must be at least " + MIN_TOKEN_LENGTH
                    + " characters, got " + gatewayToken.length());
        }
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
