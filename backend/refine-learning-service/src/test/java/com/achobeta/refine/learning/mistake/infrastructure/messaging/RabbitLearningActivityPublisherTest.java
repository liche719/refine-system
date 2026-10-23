package com.achobeta.refine.learning.mistake.infrastructure.messaging;

import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RabbitLearningActivityPublisherTest {

    private static final Executor DIRECT = Runnable::run;
    private static final long CONFIRM_TIMEOUT_MS = 1_000L;

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
        RabbitLearningActivityPublisher publisher =
                new RabbitLearningActivityPublisher(rabbitTemplate, meterRegistry, DIRECT, CONFIRM_TIMEOUT_MS);
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
        assertThat(meterRegistry.get("refine.domain.event.publish.queued").counter().count()).isEqualTo(1);
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
        RabbitLearningActivityPublisher publisher =
                new RabbitLearningActivityPublisher(rabbitTemplate, meterRegistry, DIRECT, CONFIRM_TIMEOUT_MS);

        publisher.publishAfterCommit("user-1",
                new LearningActivityPayload("question-1", "reviewed", "question", "math", 7));

        verify(rabbitTemplate, times(3)).convertAndSend(eq(EventTopics.EXCHANGE),
                eq(EventTopics.LEARNING_ACTIVITY_RECORDED), any(Object.class), any(CorrelationData.class));
        assertThat(meterRegistry.get("refine.domain.event.publish.failures").counter().count()).isEqualTo(1);
    }

    @Test
    void recordsRejectionInsteadOfBlockingWhenQueueIsFull() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        Executor rejecting = command -> {
            throw new RejectedExecutionException("queue is full");
        };
        RabbitLearningActivityPublisher publisher =
                new RabbitLearningActivityPublisher(rabbitTemplate, meterRegistry, rejecting, CONFIRM_TIMEOUT_MS);

        publisher.publishAfterCommit("user-1",
                new LearningActivityPayload("question-1", "reviewed", "question", "math", 7));

        assertThat(meterRegistry.get("refine.domain.event.publish.rejected").counter().count()).isEqualTo(1);
        verify(rabbitTemplate, times(0)).convertAndSend(any(String.class), any(String.class),
                any(Object.class), any(CorrelationData.class));
    }
}
