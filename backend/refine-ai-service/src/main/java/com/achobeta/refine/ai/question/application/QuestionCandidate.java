package com.achobeta.refine.ai.question.application;

public record QuestionCandidate(String questionId, String userId, String content, String answer, String analysis,
                                String subject, Integer knowledgePointId) { }
