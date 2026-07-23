package com.achobeta.refine.ai.question.application.port;

import com.achobeta.refine.ai.question.application.QuestionCandidate;

import java.time.Duration;

public interface QuestionCache {
    void save(QuestionCandidate candidate, Duration ttl);
    QuestionCandidate find(String questionId);
    void delete(String questionId);
}
