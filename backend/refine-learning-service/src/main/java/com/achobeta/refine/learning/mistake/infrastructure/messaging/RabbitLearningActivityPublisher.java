package com.achobeta.refine.learning.mistake.infrastructure.messaging;

import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.achobeta.refine.learning.mistake.application.port.LearningActivityPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitLearningActivityPublisher implements LearningActivityPublisher {
    private static final int MAX_ATTEMPTS = 3;
    private static final Logger log = LoggerFactory.getLogger(RabbitLearningActivityPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final Counter failureCounter;
    private final Counter queuedCounter;
    private final Counter rejectedCounter;
    private final Executor executor;
    private final long confirmTimeoutMillis;

    public RabbitLearningActivityPublisher(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry,
                                           @Qualifier("learningEventExecutor") Executor executor,
                                           @Value("${refine.events.confirm-timeout-ms:1000}") long confirmTimeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.executor = executor;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
        this.failureCounter = Counter.builder("refine.domain.event.publish.failures")
                .tag("eventType", EventTopics.LEARNING_ACTIVITY_RECORDED)
                .register(meterRegistry);
        this.queuedCounter = Counter.builder("refine.domain.event.publish.queued")
                .tag("eventType", EventTopics.LEARNING_ACTIVITY_RECORDED)
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("refine.domain.event.publish.rejected")
                .tag("eventType", EventTopics.LEARNING_ACTIVITY_RECORDED)
                .register(meterRegistry);
    }

    @Override
    public void publishAfterCommit(String userId, LearningActivityPayload payload) {
        EventEnvelope<LearningActivityPayload> event = EventEnvelope.create(
                EventTopics.LEARNING_ACTIVITY_RECORDED, userId, payload);
        Runnable dispatch = () -> dispatch(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    /** 提交后异步投递，避免请求线程等 broker confirm。 */
    private void dispatch(EventEnvelope<LearningActivityPayload> event) {
        try {
            executor.execute(() -> publish(event));
            queuedCounter.increment();
        } catch (RejectedExecutionException exception) {
            rejectedCounter.increment();
            log.error("Learning event dropped because dispatch queue is full; eventId={}, eventType={}, userId={}",
                    event.eventId(), event.eventType(), event.userId(), exception);
        }
    }

    private void publish(EventEnvelope<LearningActivityPayload> event) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                CorrelationData correlation = new CorrelationData(event.eventId().toString());
                rabbitTemplate.convertAndSend(EventTopics.EXCHANGE, EventTopics.LEARNING_ACTIVITY_RECORDED,
                        event, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture()
                        .get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
                if (confirm.isAck()) {
                    return;
                }
                lastFailure = new IllegalStateException("Broker nack: " + confirm.getReason());
            } catch (Exception exception) {
                lastFailure = exception;
            }
            log.warn("Learning event publish attempt failed; eventId={}, userId={}, questionId={}, attempt={}",
                    event.eventId(), event.userId(), event.payload().questionId(), attempt, lastFailure);
        }
        failureCounter.increment();
        log.error("Learning event publish failed; eventId={}, eventType={}, userId={}, questionId={}, attempts={}",
                event.eventId(), event.eventType(), event.userId(), event.payload().questionId(), MAX_ATTEMPTS,
                lastFailure);
    }
}
