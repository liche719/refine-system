package com.achobeta.refine.contracts.learning;

import jakarta.validation.constraints.NotBlank;

public record EnsureKnowledgePointRequest(
        @NotBlank String userId,
        @NotBlank String name,
        String description,
        @NotBlank String subject) {
}
