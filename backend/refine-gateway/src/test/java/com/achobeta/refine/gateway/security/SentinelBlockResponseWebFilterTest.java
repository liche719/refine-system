package com.achobeta.refine.gateway.security;

import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelBlockResponseWebFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SentinelBlockResponseWebFilter filter = new SentinelBlockResponseWebFilter(
            new GatewaySentinelExceptionHandler(objectMapper));

    @Test
    void convertsDirectParamFlowExceptionToUnifiedTooManyRequestsResponse() throws Exception {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(filter.filter(exchange,
                        ignored -> Mono.error(new ParamFlowException("ai-service", "userId"))))
                .verifyComplete();

        assertTooManyRequests(exchange);
    }

    @Test
    void convertsWrappedParamFlowExceptionToUnifiedTooManyRequestsResponse() throws Exception {
        MockServerWebExchange exchange = exchange();
        RuntimeException wrapped = new RuntimeException(
                "reactive invocation failed", new ParamFlowException("ai-service", "userId"));

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.error(wrapped)))
                .verifyComplete();

        assertTooManyRequests(exchange);
    }

    @Test
    void propagatesNonSentinelFailures() {
        MockServerWebExchange exchange = exchange();
        IllegalStateException failure = new IllegalStateException("downstream failed");

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.error(failure)))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/ai_suggession/get_key_point").build());
    }

    private void assertTooManyRequests(MockServerWebExchange exchange) throws Exception {
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.path("traceId").asText()).isEqualTo(exchange.getRequest().getId());
        assertThat(body.path("code").asInt()).isEqualTo(429);
        assertThat(body.path("info").asText()).isEqualTo("请求过于频繁，请稍后重试");
        assertThat(body.path("data").isNull()).isTrue();
    }
}
