package com.achobeta.refine.ai.analytics.application;

import com.achobeta.refine.ai.analytics.application.port.AnalyticsRepository;
import com.achobeta.refine.ai.analytics.application.query.InsightRow;
import com.achobeta.refine.ai.analytics.application.query.LearningVectorRow;
import com.achobeta.refine.ai.analytics.application.query.WeaknessRow;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import com.achobeta.refine.common.datasource.ReadReplica;
import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.achobeta.refine.contracts.event.UserLoggedInPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningAnalysisService {
    private static final int SEARCH_CANDIDATES = 200;
    private static final int CURRENT_EVENT_VERSION = 1;
    private static final int MAX_USER_ID_LENGTH = 50;

    private final AnalyticsRepository repository;
    private final ObjectMapper objectMapper;
    private final TextEmbeddingPort embeddings;
    private final VectorCodec vectors;

    public LearningAnalysisService(AnalyticsRepository repository, ObjectMapper objectMapper,
                                   TextEmbeddingPort embeddings, VectorCodec vectors) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.embeddings = embeddings;
        this.vectors = vectors;
    }

    @Transactional
    public boolean consume(EventEnvelope<LearningActivityPayload> event) {
        validateEnvelope(event, EventTopics.LEARNING_ACTIVITY_RECORDED);
        if (!repository.markConsumed(event.eventId().toString(), event.eventType())) {
            return false;
        }
        LearningActivityPayload payload = event.payload();
        String vectorText = String.join(" ", safe(payload.actionType()), safe(payload.subject()),
                safe(payload.questionContent()));
        repository.insertVector(event.eventId().toString(), event.userId(), payload.questionId(), payload.actionType(),
                payload.questionContent(), payload.subject(), payload.knowledgePointId(),
                vectors.serialize(embeddings.embed(vectorText)), embeddings.modelName(), metadata(payload));
        rebuildInsights(event.userId());
        return true;
    }

    @Transactional
    public boolean consumeLogin(EventEnvelope<UserLoggedInPayload> event) {
        validateEnvelope(event, EventTopics.USER_LOGGED_IN);
        if (!repository.markConsumed(event.eventId().toString(), event.eventType())) {
            return false;
        }
        rebuildInsights(event.userId());
        return true;
    }

    @Transactional
    public boolean trigger(String userId) {
        rebuildInsights(userId);
        return true;
    }

    @ReadReplica
    public List<Insight> insights(String userId, String type) {
        return repository.findInsights(userId, type).stream().map(this::toInsight).toList();
    }

    @ReadReplica
    public List<SimilarQuestion> similarQuestions(String userId, String queryText, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), 20);
        double[] query = embeddings.embed(queryText);
        return repository.recentVectors(userId, SEARCH_CANDIDATES).stream()
                .filter(row -> embeddings.modelName().equals(row.embeddingModel()))
                .map(row -> new SimilarQuestion(row.questionId(), row.questionContent(), row.actionType(), row.subject(),
                        vectors.cosine(query, vectors.parse(row.embeddingText())),
                        row.createdAt() == null ? null : row.createdAt().toString()))
                .sorted(Comparator.comparingDouble(SimilarQuestion::similarity).reversed())
                .limit(limit)
                .toList();
    }

    @ReadReplica
    public List<LearningDynamic> dynamics(String userId) {
        return repository.recentVectors(userId, 20).stream().map(row -> {
            String actionType = row.actionType();
            String subject = row.subject();
            return new LearningDynamic(actionType, actionTitle(actionType),
                    safe(row.questionContent()), subject, priority(actionType),
                    "mistake".equals(actionType) ? "安排一次针对性复习" : "继续巩固相关知识点", 1);
        }).toList();
    }

    private void rebuildInsights(String userId) {
        repository.deleteInsights(userId);
        List<WeaknessRow> weaknesses = repository.weaknessSubjects(userId);
        for (WeaknessRow row : weaknesses) {
            long count = row.itemCount();
            if (count < 3) {
                continue;
            }
            String subject = row.subject();
            saveInsight(userId, "weakness", subject + "学科薄弱",
                    "在" + subject + "学科中累计记录" + count + "道错题，建议加强练习",
                    Math.min(0.9D, 0.5D + count * 0.1D), repository.recentQuestionIds(userId, subject));
        }

        long recentWeekCount = repository.recentWeekCount(userId);
        if (recentWeekCount == 0) {
            saveInsight(userId, "recommendation", "开始学习之旅", "暂无学习记录，建议先上传题目进行学习",
                    1D, List.of());
        } else if (recentWeekCount < 3) {
            saveInsight(userId, "recommendation", "增加学习频率", "建议每天安排至少30分钟学习时间",
                    0.8D, List.of());
        } else {
            saveInsight(userId, "recommendation", "保持学习节奏", "近一周学习活跃，建议继续保持",
                    Math.min(0.95D, 0.7D + recentWeekCount * 0.01D), List.of());
        }
        if (recentWeekCount > 15) {
            saveInsight(userId, "strength", "学习积极性高", "近一周学习非常活跃",
                    0.9D, List.of());
        }
        long activeDays = repository.recentActiveDays(userId);
        if (activeDays >= 5) {
            saveInsight(userId, "achievement", "学习坚持性优秀", "近一周有" + activeDays + "天进行了学习",
                    0.8D, List.of());
        }
    }

    private void validateEnvelope(EventEnvelope<?> event, String expectedEventType) {
        if (event == null) {
            throw new IllegalArgumentException("Event envelope must not be null");
        }
        if (event.eventId() == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }
        if (!expectedEventType.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type: " + event.eventType());
        }
        if (event.version() != CURRENT_EVENT_VERSION) {
            throw new IllegalArgumentException("Unsupported event version: " + event.version());
        }
        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("Event occurrence time must not be null");
        }
        if (event.payload() == null) {
            throw new IllegalArgumentException("Event payload must not be null");
        }
        String userId = event.userId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Event user id must not be blank");
        }
        if (userId.length() > MAX_USER_ID_LENGTH) {
            throw new IllegalArgumentException("Event user id exceeds " + MAX_USER_ID_LENGTH + " characters");
        }
    }

    private void saveInsight(String userId, String type, String title, String description,
                             double confidence, List<String> relatedQuestions) {
        repository.insertInsight(userId, type, title, description, confidence, json(relatedQuestions));
    }

    private String metadata(LearningActivityPayload payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("actionType", payload.actionType());
        value.put("subject", payload.subject());
        value.put("knowledgePointId", payload.knowledgePointId());
        return json(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize learning analysis metadata", exception);
        }
    }

    private Insight toInsight(InsightRow row) {
        List<String> related = List.of();
        String json = row.relatedQuestions();
        if (json != null && !json.isBlank()) {
            try {
                related = objectMapper.readValue(json, new TypeReference<List<String>>() { });
            } catch (JsonProcessingException ignored) {
                related = List.of();
            }
        }
        return new Insight(row.type(), row.title(), row.description(), row.confidenceScore(), related,
                row.createdAt() == null ? null : row.createdAt().toString(), row.active());
    }

    private String actionTitle(String actionType) {
        return switch (safe(actionType)) {
            case "mistake" -> "新增错题";
            case "upload" -> "上传题目";
            case "review" -> "完成复习";
            case "qa" -> "完成问答";
            default -> "学习记录";
        };
    }

    private int priority(String actionType) {
        return "mistake".equals(actionType) ? 3 : "review".equals(actionType) ? 1 : 2;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record Insight(String type, String title, String description, Double confidenceScore,
                          List<String> relatedQuestions, String createdAt, Boolean isActive) { }

    public record SimilarQuestion(String questionId, String questionContent, String actionType, String subject,
                                  double similarity, String createdAt) { }

    public record LearningDynamic(String type, String title, String description, String subject, Integer priority,
                                  String suggestion, Integer relatedQuestionCount) { }
}
