package com.achobeta.refine.gateway.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";
    private static final String GATEWAY_TOKEN = "gateway-token-for-tests";
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            new GatewaySecurityProperties(SECRET, GATEWAY_TOKEN, List.of("/api/userAccount/login")), new ObjectMapper());

    @Test
    void rejectsUnresolvedJwtSecretAtStartup() {
        assertThatThrownBy(() -> new GatewaySecurityProperties("${JWT_SECRET}", GATEWAY_TOKEN, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void replacesSpoofedIdentityWithVerifiedClaim() {
        String token = JWT.create()
                .withClaim("type", "access")
                .withClaim("userId", "real-user")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/overview/get_overview")
                .header("access-token", token)
                .header(JwtAuthenticationFilter.USER_ID, "spoofed")
                .header(JwtAuthenticationFilter.INTERNAL_TOKEN, "spoofed")
                .header(JwtAuthenticationFilter.GATEWAY_TOKEN, "spoofed")
                .build());
        AtomicReference<HttpHeaders> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = value -> {
            forwarded.set(value.getRequest().getHeaders());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwarded.get().getFirst(JwtAuthenticationFilter.USER_ID)).isEqualTo("real-user");
        assertThat(forwarded.get().containsKey(JwtAuthenticationFilter.INTERNAL_TOKEN)).isFalse();
        assertThat(forwarded.get().getFirst(JwtAuthenticationFilter.GATEWAY_TOKEN)).isEqualTo(GATEWAY_TOKEN);
    }

    @Test
    void rejectsExpiredToken() {
        String token = JWT.create()
                .withClaim("type", "access")
                .withClaim("userId", "user")
                .withExpiresAt(Date.from(Instant.now().minusSeconds(1)))
                .sign(Algorithm.HMAC256(SECRET));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/question/generation")
                .header("access-token", token).build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"code\":401", "\"data\":null");
    }

    @Test
    void rejectsMissingTokenWithUnauthorizedResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/learning-analysis/insights").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"code\":401");
    }
}
