package com.achobeta.refine.learning.mistake.infrastructure.messaging;

import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RabbitLearningActivityPublisherTest {

    @Test
    void publishesWithExpectedContractAndStopsAfterAck() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EventTopics.EXCHANGE),
                eq(EventTopics.LEARNING_ACTIVITY_RECORDED), any(Object.class), any(CorrelationData.class));
        RabbitLearningActivityPublisher publisher = new RabbitLearningActivityPublisher(rabbitTemplate, meterRegistry);
        LearningActivityPayload payload = new LearningActivityPayload(
                "question-1", "reviewed", "question", "math", 7);

        publisher.publishAfterCommit("user-1", payload);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(EventTopics.EXCHANGE),
                eq(EventTopics.LEARNING_ACTIVITY_RECORDED), eventCaptor.capture(), any(CorrelationData.class));
        @SuppressWarnings("unchecked")
        EventEnvelope<LearningActivityPayload> event = (EventEnvelope<LearningActivityPayload>) eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo(EventTopics.LEARNING_ACTIVITY_RECORDED);
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.payload()).isEqualTo(payload);
        assertThat(meterRegistry.get("refine.domain.event.publish.failures").counter().count()).isZero();
    }

    @Test
    void retriesThreeTimesAndRecordsFailureAfterNack() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "unroutable"));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EventTopics.EXCHANGE),
                eq(EventTopics.LEARNING_ACTIVITY_RECORDED), any(Object.class), any(CorrelationData.class));
        RabbitLearningActivityPublisher publisher = new RabbitLearningActivityPublisher(rabbitTemplate, meterRegistry);

        publisher.publishAfterCommit("user-1",
                new LearningActivityPayload("question-1", "reviewed", "question", "math", 7));

        verify(rabbitTemplate, times(3)).convertAndSend(eq(EventTopics.EXCHANGE),
                eq(EventTopics.LEARNING_ACTIVITY_RECORDED), any(Object.class), any(CorrelationData.class));
        assertThat(meterRegistry.get("refine.domain.event.publish.failures").counter().count()).isEqualTo(1);
    }
}
