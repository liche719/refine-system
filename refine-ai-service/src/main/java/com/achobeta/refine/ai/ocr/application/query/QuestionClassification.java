package com.achobeta.refine.ai.ocr.application.query;

public record QuestionClassification(String subject, String knowledgePoint, String description) {
    public boolean isUsable() {
        return subject != null && !subject.isBlank() && knowledgePoint != null && !knowledgePoint.isBlank();
    }
}
