package com.achobeta.refine.ai.analytics.application;

import com.achobeta.refine.ai.analytics.application.port.AnalyticsRepository;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import com.achobeta.refine.common.datasource.ReadReplica;
import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.achobeta.refine.contracts.event.UserLoggedInPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LearningAnalysisServiceTest {
    @Test
    void storesRemoteEmbeddingAndBuildsRecommendationOnce() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        when(repository.markConsumed(anyString(), anyString())).thenReturn(true);
        when(repository.weaknessSubjects("u1")).thenReturn(List.of());
        when(repository.recentWeekCount("u1")).thenReturn(1L);
        LearningAnalysisService service = service(repository);
        EventEnvelope<LearningActivityPayload> event = event();

        assertThat(service.consume(event)).isTrue();

        verify(repository).insertVector(eq(event.eventId().toString()), eq("u1"), eq("q1"), eq("mistake"),
                eq("2 + 2 = 5"), eq("math"), eq(1), anyString(), eq("text-embedding-v4"), anyString());
        verify(repository).insertInsight(eq("u1"), eq("recommendation"), anyString(),
                anyString(), eq(0.8D), eq("[]"));
    }

    @Test
    void duplicateEventDoesNotPersistVectorOrInsights() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        when(repository.markConsumed(anyString(), anyString())).thenReturn(false);
        TextEmbeddingPort embeddings = embeddings();
        LearningAnalysisService service = new LearningAnalysisService(
                repository, new ObjectMapper(), embeddings, new VectorCodec(), inlineTransactionTemplate());

        assertThat(service.consume(event())).isFalse();

        // embedding 在事务外计算（避免远程调用占住数据库连接），所以重复投递仍会算一次向量；
        // 但去重之后不会写向量、不会重算洞察，这才是幂等要保证的部分。
        verify(repository, never()).insertVector(anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyString(), anyString(), anyString());
        verify(repository, never()).deleteInsights(anyString());
    }

    @Test
    void rejectsMalformedActivityEnvelopesBeforeWritingConsumptionMarker() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        LearningAnalysisService service = service(repository);
        LearningActivityPayload payload = new LearningActivityPayload("q1", "mistake", "question", "math", 1);
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        assertThatThrownBy(() -> service.consume(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("envelope");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(null,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, occurredAt, "u1", payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event id");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(eventId,
                EventTopics.USER_LOGGED_IN, 1, occurredAt, "u1", payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event type");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(eventId,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 2, occurredAt, "u1", payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(eventId,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, null, "u1", payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occurrence time");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<LearningActivityPayload>(eventId,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, occurredAt, "u1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(eventId,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, occurredAt, " ", payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user id");
        assertThatThrownBy(() -> service.consume(new EventEnvelope<>(eventId,
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, occurredAt, "u".repeat(51), payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsActivityTopicOnLoginConsumerBeforeWritingConsumptionMarker() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        LearningAnalysisService service = service(repository);
        EventEnvelope<UserLoggedInPayload> event = new EventEnvelope<>(UUID.randomUUID(),
                EventTopics.LEARNING_ACTIVITY_RECORDED, 1, Instant.now(), "u1",
                new UserLoggedInPayload(Instant.now()));

        assertThatThrownBy(() -> service.consumeLogin(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event type");
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsOlderEventVersionsAndRejectsUnknownNewerOnes() {
        // 消费者升级到 v2 之后，队列里在途的 v1 消息仍应被消费，而不是全部进死信
        assertThat(LearningAnalysisService.isSupportedVersion(1, 2)).isTrue();
        assertThat(LearningAnalysisService.isSupportedVersion(2, 2)).isTrue();
        assertThat(LearningAnalysisService.isSupportedVersion(3, 2)).isFalse();
        assertThat(LearningAnalysisService.isSupportedVersion(0, 2)).isFalse();
    }

    @Test
    void routesOnlyEventuallyConsistentQueriesToReadReplica() throws NoSuchMethodException {
        assertThat(LearningAnalysisService.class.getMethod("insights", String.class, String.class)
                .isAnnotationPresent(ReadReplica.class)).isTrue();
        assertThat(LearningAnalysisService.class.getMethod("similarQuestions", String.class, String.class, int.class)
                .isAnnotationPresent(ReadReplica.class)).isTrue();
        assertThat(LearningAnalysisService.class.getMethod("dynamics", String.class)
                .isAnnotationPresent(ReadReplica.class)).isTrue();

        assertThat(LearningAnalysisService.class.getMethod("consume", EventEnvelope.class)
                .isAnnotationPresent(ReadReplica.class)).isFalse();
        assertThat(LearningAnalysisService.class.getMethod("consumeLogin", EventEnvelope.class)
                .isAnnotationPresent(ReadReplica.class)).isFalse();
        assertThat(LearningAnalysisService.class.getMethod("trigger", String.class)
                .isAnnotationPresent(ReadReplica.class)).isFalse();
    }

    private LearningAnalysisService service(AnalyticsRepository repository) {
        return new LearningAnalysisService(repository, new ObjectMapper(), embeddings(), new VectorCodec(),
                inlineTransactionTemplate());
    }

    @SuppressWarnings("unchecked")
    private TransactionTemplate inlineTransactionTemplate() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        doAnswer(invocation -> ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null))
                .when(template).execute(any(TransactionCallback.class));
        return template;
    }

    private TextEmbeddingPort embeddings() {
        TextEmbeddingPort embeddings = mock(TextEmbeddingPort.class);
        when(embeddings.embed(anyString())).thenReturn(new double[]{1D, 0D});
        when(embeddings.modelName()).thenReturn("text-embedding-v4");
        return embeddings;
    }

    private EventEnvelope<LearningActivityPayload> event() {
        return new EventEnvelope<>(UUID.randomUUID(), EventTopics.LEARNING_ACTIVITY_RECORDED, 1, Instant.now(),
                "u1", new LearningActivityPayload("q1", "mistake", "2 + 2 = 5", "math", 1));
    }
}
