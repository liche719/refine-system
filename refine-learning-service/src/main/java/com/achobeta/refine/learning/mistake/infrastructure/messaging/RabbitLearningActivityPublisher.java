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
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Component
public class RabbitLearningActivityPublisher implements LearningActivityPublisher {
    private static final int MAX_ATTEMPTS = 3;
    private static final long CONFIRM_TIMEOUT_SECONDS = 3;
    private static final Logger log = LoggerFactory.getLogger(RabbitLearningActivityPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final Counter failureCounter;

    public RabbitLearningActivityPublisher(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.failureCounter = Counter.builder("refine.domain.event.publish.failures")
                .tag("eventType", EventTopics.LEARNING_ACTIVITY_RECORDED)
                .register(meterRegistry);
    }

    @Override
    public void publishAfterCommit(String userId, LearningActivityPayload payload) {
        EventEnvelope<LearningActivityPayload> event = EventEnvelope.create(
                EventTopics.LEARNING_ACTIVITY_RECORDED, userId, payload);
        Runnable publish = () -> publish(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
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
                        .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
