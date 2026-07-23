package com.achobeta.refine.ai.analytics.application.query;

import java.time.LocalDateTime;

public record LearningVectorRow(String questionId, String questionContent, String actionType, String subject,
                                String embeddingText, String embeddingModel, LocalDateTime createdAt) { }
