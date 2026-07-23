package com.achobeta.refine.ai.question.infrastructure;

import com.achobeta.refine.ai.question.application.QuestionCandidate;
import com.achobeta.refine.ai.question.application.port.QuestionCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisQuestionCache implements QuestionCache {
    private static final String PREFIX = "ai:generated-question:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    public RedisQuestionCache(StringRedisTemplate redis, ObjectMapper objectMapper) { this.redis = redis; this.objectMapper = objectMapper; }
    @Override public void save(QuestionCandidate candidate, Duration ttl) {
        try { redis.opsForValue().set(PREFIX + candidate.questionId(), objectMapper.writeValueAsString(candidate), ttl); }
        catch (Exception exception) { throw new IllegalStateException("Generated question persistence failed", exception); }
    }
    @Override public QuestionCandidate find(String questionId) {
        try {
            String json = redis.opsForValue().get(PREFIX + questionId);
            return json == null ? null : objectMapper.readValue(json, QuestionCandidate.class);
        } catch (Exception exception) { throw new IllegalStateException("Generated question cache is corrupted", exception); }
    }
    @Override public void delete(String questionId) { redis.delete(PREFIX + questionId); }
}
