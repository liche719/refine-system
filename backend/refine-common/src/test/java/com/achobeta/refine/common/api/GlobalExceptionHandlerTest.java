package com.achobeta.refine.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSecurityAndGovernanceCodesToTheirHttpStatuses() {
        assertThat(handler.handleAppException(new AppException(401, "unauthorized")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleAppException(new AppException(429, "limited")).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(handler.handleAppException(new AppException(503, "unavailable")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(handler.handleAppException(new AppException(1001, "invalid")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
