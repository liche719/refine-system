package com.achobeta.refine.contracts.learning;

public record GenerationContextResponse(
        long mistakeQuestionId,
        String subject,
        Integer knowledgePointId,
        String knowledgePointName,
        String questionContent) {
}
