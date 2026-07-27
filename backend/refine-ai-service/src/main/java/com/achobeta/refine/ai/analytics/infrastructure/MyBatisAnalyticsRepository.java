package com.achobeta.refine.ai.analytics.infrastructure;

import com.achobeta.refine.ai.analytics.application.port.AnalyticsRepository;
import com.achobeta.refine.ai.analytics.application.query.InsightRow;
import com.achobeta.refine.ai.analytics.application.query.LearningVectorRow;
import com.achobeta.refine.ai.analytics.application.query.WeaknessRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MyBatisAnalyticsRepository implements AnalyticsRepository {
    private final AnalyticsMapper mapper;
    public MyBatisAnalyticsRepository(AnalyticsMapper mapper) { this.mapper = mapper; }
    @Override
    public boolean markConsumed(String eventId, String eventType) {
        try {
            return mapper.markConsumed(eventId, eventType) > 0;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
    @Override public void insertVector(String eventId, String userId, String questionId, String actionType,
                                       String questionContent, String subject, Integer knowledgePointId,
                                       String embeddingText, String embeddingModel, String metadataText) {
        mapper.insertVector(eventId, userId, questionId, actionType, questionContent, subject, knowledgePointId,
                embeddingText, embeddingModel, metadataText);
    }
    @Override public void deleteInsights(String userId) { mapper.deleteInsights(userId); }
    @Override public void insertInsight(String userId, String type, String title, String description,
                                        double confidence, String metadata) {
        mapper.insertInsight(userId, type, title, description, confidence, metadata);
    }
    @Override public List<InsightRow> findInsights(String userId, String type) { return mapper.findInsights(userId, type); }
    @Override public List<LearningVectorRow> recentVectors(String userId, int limit) { return mapper.recentVectors(userId, limit); }
    @Override public List<WeaknessRow> weaknessSubjects(String userId) { return mapper.weaknessSubjects(userId); }
    @Override public long recentWeekCount(String userId) { return mapper.recentWeekCount(userId); }
    @Override public long recentActiveDays(String userId) { return mapper.recentActiveDays(userId); }
    @Override public List<String> recentQuestionIds(String userId, String subject) { return mapper.recentQuestionIds(userId, subject); }
}
