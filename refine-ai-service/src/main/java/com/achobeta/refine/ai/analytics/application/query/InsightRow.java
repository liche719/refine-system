package com.achobeta.refine.ai.analytics.application.query;

import java.time.LocalDateTime;

public record InsightRow(String type, String title, String description, double confidenceScore,
                         String relatedQuestions, LocalDateTime createdAt, boolean active) { }
