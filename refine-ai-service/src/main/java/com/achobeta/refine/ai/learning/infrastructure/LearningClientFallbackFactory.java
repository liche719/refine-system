package com.achobeta.refine.ai.learning.infrastructure;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningClientFallbackFactory implements FallbackFactory<LearningClient> {
    private static final Logger log = LoggerFactory.getLogger(LearningClientFallbackFactory.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public LearningClient create(Throwable cause) {
        AppException businessFailure = businessFailure(cause);
        if (businessFailure == null) {
            log.error("Learning service call degraded", cause);
        } else {
            log.warn("Learning service rejected a request: {}", businessFailure.getMessage());
        }
        return new LearningClient() {
            @Override
            public CreateMistakeResponse createMistake(CreateMistakeRequest request) {
                throw failure(businessFailure);
            }

            @Override
            public EnsureKnowledgePointResponse ensureKnowledgePoint(EnsureKnowledgePointRequest request) {
                throw failure(businessFailure);
            }

            @Override
            public GenerationContextResponse generationContext(long id, String userId) {
                throw failure(businessFailure);
            }

            @Override
            public List<RecentKnowledgePoint> recentKnowledge(String userId, int limit) {
                return List.of();
            }
        };
    }

    private static AppException failure(AppException businessFailure) {
        return businessFailure == null ? unavailable() : businessFailure;
    }

    private static AppException unavailable() {
        return new AppException(503, "学习服务暂时不可用");
    }

    private static AppException businessFailure(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (!(current instanceof FeignException exception)
                    || exception.status() < 400 || exception.status() >= 500) {
                continue;
            }
            try {
                JsonNode response = OBJECT_MAPPER.readTree(exception.contentUTF8());
                int code = response.path("code").asInt(10001);
                String message = response.path("info").asText("learning request was rejected");
                return new AppException(code, message);
            } catch (Exception ignored) {
                return new AppException(10001, "learning request was rejected");
            }
        }
        return null;
    }
}
