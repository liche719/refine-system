package com.achobeta.refine.contracts.event;

public record LearningActivityPayload(
        String questionId,
        String actionType,
        String questionContent,
        String subject,
        Integer knowledgePointId) {
}
