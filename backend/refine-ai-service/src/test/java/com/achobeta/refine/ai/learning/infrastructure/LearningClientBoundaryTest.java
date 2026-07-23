package com.achobeta.refine.ai.learning.infrastructure;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

class LearningClientBoundaryTest {
    @Test
    void fallbackFailsWritesFastAndDegradesOptionalRecentKnowledge() {
        LearningClient fallback = new LearningClientFallbackFactory().create(new IllegalStateException("timeout"));

        assertThat(fallback.recentKnowledge("user-1", 5)).isEmpty();
        assertThatThrownBy(() -> fallback.createMistake(
                new CreateMistakeRequest("user-1", "question-1", "question", "math", 7, "ocr")))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(503));
    }

    @Test
    void feignTimeoutsRemainFiniteAndExplicit() throws Exception {
        var propertySource = new YamlPropertySourceLoader()
                .load("ai", new ClassPathResource("application.yml")).getFirst();

        assertThat(propertySource.getProperty(
                "spring.cloud.openfeign.client.config.default.connect-timeout")).isEqualTo(3000);
        assertThat(propertySource.getProperty(
                "spring.cloud.openfeign.client.config.default.read-timeout")).isEqualTo(10000);
    }

    @Test
    void fallbackPreservesLearningBusinessErrorsInsteadOfReportingAnOutage() {
        Request request = Request.create(Request.HttpMethod.GET, "http://learning/internal/test", Map.of(), null,
                StandardCharsets.UTF_8);
        Response response = Response.builder().status(400).reason("Bad Request").request(request)
                .headers(Map.of()).body("{\"code\":10002,\"info\":\"mistake question not found\",\"data\":null}",
                        StandardCharsets.UTF_8)
                .build();
        LearningClient fallback = new LearningClientFallbackFactory()
                .create(FeignException.errorStatus("LearningClient#generationContext", response));

        assertThatThrownBy(() -> fallback.generationContext(35L, "user-1"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(10002);
                    assertThat(exception.getMessage()).isEqualTo("mistake question not found");
                });
    }
}
