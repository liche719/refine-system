package com.achobeta.refine.ai.analytics.application.port;

import com.achobeta.refine.ai.analytics.application.query.InsightRow;
import com.achobeta.refine.ai.analytics.application.query.LearningVectorRow;
import com.achobeta.refine.ai.analytics.application.query.WeaknessRow;

import java.util.List;

public interface AnalyticsRepository {
    boolean markConsumed(String eventId, String eventType);
    void insertVector(String eventId, String userId, String questionId, String actionType, String questionContent,
                      String subject, Integer knowledgePointId, String embeddingText, String embeddingModel,
                      String metadataText);
    void deleteInsights(String userId);
    void insertInsight(String userId, String type, String title, String description, double confidence, String metadata);
    List<InsightRow> findInsights(String userId, String type);
    List<LearningVectorRow> recentVectors(String userId, int limit);
    List<WeaknessRow> weaknessSubjects(String userId);
    long recentWeekCount(String userId);
    long recentActiveDays(String userId);
    List<String> recentQuestionIds(String userId, String subject);
}
