package com.achobeta.refine.contracts.learning;

import jakarta.validation.constraints.NotBlank;

public record CreateMistakeRequest(
        @NotBlank String userId,
        String questionId,
        @NotBlank String questionContent,
        String subject,
        Integer knowledgePointId,
        @NotBlank String source) {
}
