package com.achobeta.refine.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Ensures Sentinel blocks use the gateway JSON contract inside the reactive chain. */
@Component
public class SentinelBlockResponseWebFilter implements WebFilter, Ordered {
    private final GatewaySentinelExceptionHandler exceptionHandler;

    public SentinelBlockResponseWebFilter(GatewaySentinelExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(exception -> exceptionHandler.handle(exchange, exception));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
