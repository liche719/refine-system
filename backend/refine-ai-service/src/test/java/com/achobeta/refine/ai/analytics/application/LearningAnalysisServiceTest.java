package com.achobeta.refine.ai.analytics.application;

import com.achobeta.refine.ai.analytics.application.port.AnalyticsRepository;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void duplicateEventDoesNotCallRemoteEmbedding() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        when(repository.markConsumed(anyString(), anyString())).thenReturn(false);
        TextEmbeddingPort embeddings = embeddings();
        LearningAnalysisService service = new LearningAnalysisService(
                repository, new ObjectMapper(), embeddings, new VectorCodec());

        assertThat(service.consume(event())).isFalse();

        verify(embeddings, never()).embed(anyString());
        verify(repository, never()).insertVector(anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyString(), anyString(), anyString());
    }

    private LearningAnalysisService service(AnalyticsRepository repository) {
        return new LearningAnalysisService(repository, new ObjectMapper(), embeddings(), new VectorCodec());
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
