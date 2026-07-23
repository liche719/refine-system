package com.achobeta.refine.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class GatewayTokenFilter extends OncePerRequestFilter {
    private final byte[] expectedToken;

    public GatewayTokenFilter(@Value("${refine.security.gateway-token}") String expectedToken) {
        if (expectedToken == null || expectedToken.isBlank() || expectedToken.contains("${")) {
            throw new IllegalStateException("GATEWAY_TOKEN must be configured with a resolved value");
        }
        this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/internal/") || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(IdentityHeaders.GATEWAY_TOKEN);
        byte[] providedBytes = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, providedBytes)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway token");
            return;
        }
        chain.doFilter(request, response);
    }
}
