package com.achobeta.refine.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "refine.gateway.security")
public record GatewaySecurityProperties(String jwtSecret, String gatewayToken, List<String> publicPaths) {
    public GatewaySecurityProperties {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("${")) {
            throw new IllegalStateException("JWT_SECRET must be configured with a resolved value");
        }
        if (gatewayToken == null || gatewayToken.isBlank() || gatewayToken.contains("${")) {
            throw new IllegalStateException("GATEWAY_TOKEN must be configured with a resolved value");
        }
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
