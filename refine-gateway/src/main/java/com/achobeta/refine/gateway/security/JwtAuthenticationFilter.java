package com.achobeta.refine.gateway.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    static final String USER_ID = "X-User-Id";
    static final String INTERNAL_TOKEN = "X-Internal-Token";
    static final String GATEWAY_TOKEN = "X-Gateway-Token";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final GatewaySecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final JWTVerifier verifier;

    public JwtAuthenticationFilter(GatewaySecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.verifier = JWT.require(Algorithm.HMAC256(properties.jwtSecret()))
                .withClaim("type", "access")
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate().request(request -> request.headers(headers -> {
            headers.remove(USER_ID);
            headers.remove(INTERNAL_TOKEN);
            headers.remove(GATEWAY_TOKEN);
        })).build();

        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(withGatewayToken(sanitized));
        }

        try {
            String token = resolveToken(exchange.getRequest().getHeaders());
            String userId = verifier.verify(token).getClaim("userId").asString();
            if (userId == null || userId.isBlank()) {
                return unauthorized(sanitized, "access-token 缺少用户身份");
            }
            ServerWebExchange authenticated = withGatewayToken(sanitized).mutate()
                    .request(request -> request.header(USER_ID, userId))
                    .build();
            return chain.filter(authenticated);
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            log.debug("JWT rejected for {}: {}", path, exception.getMessage());
            return unauthorized(sanitized, "access-token 无效或已过期，请使用 refresh-token 刷新");
        }
    }

    private ServerWebExchange withGatewayToken(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.header(GATEWAY_TOKEN, properties.gatewayToken()))
                .build();
    }

    private boolean isPublic(String path) {
        return path.equals("/actuator/health") || properties.publicPaths().stream().anyMatch(path::equals);
    }

    private String resolveToken(HttpHeaders headers) {
        String legacy = headers.getFirst("access-token");
        if (legacy != null && !legacy.isBlank()) {
            return legacy;
        }
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Missing access token");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(new GatewayErrorResponse(
                    exchange.getRequest().getId(), 401, message, null));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception exception) {
            byte[] bytes = ("{\"traceId\":\"" + exchange.getRequest().getId() +
                    "\",\"code\":401,\"info\":\"Unauthorized\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
