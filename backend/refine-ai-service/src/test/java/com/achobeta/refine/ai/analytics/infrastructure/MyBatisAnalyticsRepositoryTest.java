package com.achobeta.refine.ai.analytics.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyBatisAnalyticsRepositoryTest {
    @Test
    void returnsTrueWhenConsumptionMarkerIsInserted() {
        AnalyticsMapper mapper = mock(AnalyticsMapper.class);
        when(mapper.markConsumed("event-1", "event.type.v1")).thenReturn(1);
        MyBatisAnalyticsRepository repository = new MyBatisAnalyticsRepository(mapper);

        assertThat(repository.markConsumed("event-1", "event.type.v1")).isTrue();
    }

    @Test
    void returnsFalseWhenEventIdAlreadyExists() {
        AnalyticsMapper mapper = mock(AnalyticsMapper.class);
        when(mapper.markConsumed("event-1", "event.type.v1"))
                .thenThrow(new DuplicateKeyException("duplicate event id"));
        MyBatisAnalyticsRepository repository = new MyBatisAnalyticsRepository(mapper);

        assertThat(repository.markConsumed("event-1", "event.type.v1")).isFalse();
    }

    @Test
    void propagatesNonDuplicateIntegrityViolations() {
        AnalyticsMapper mapper = mock(AnalyticsMapper.class);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("event type truncated");
        when(mapper.markConsumed("event-1", "event.type.v1")).thenThrow(failure);
        MyBatisAnalyticsRepository repository = new MyBatisAnalyticsRepository(mapper);

        assertThatThrownBy(() -> repository.markConsumed("event-1", "event.type.v1"))
                .isSameAs(failure);
    }
}
