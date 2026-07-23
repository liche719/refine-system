package com.achobeta.refine.identity.account.infrastructure.messaging;

import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.EventTopics;
import com.achobeta.refine.contracts.event.UserLoggedInPayload;
import com.achobeta.refine.identity.account.application.port.LoginEventPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitLoginEventPublisher implements LoginEventPort {
    private static final int MAX_ATTEMPTS = 3;
    private static final long CONFIRM_TIMEOUT_SECONDS = 3;
    private static final Logger log = LoggerFactory.getLogger(RabbitLoginEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final Counter failureCounter;

    public RabbitLoginEventPublisher(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.failureCounter = Counter.builder("refine.domain.event.publish.failures")
                .tag("eventType", EventTopics.USER_LOGGED_IN).register(meterRegistry);
    }

    @Override
    public void publish(String userId) {
        EventEnvelope<UserLoggedInPayload> event = EventEnvelope.create(
                EventTopics.USER_LOGGED_IN, userId, new UserLoggedInPayload(Instant.now()));
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                CorrelationData correlation = new CorrelationData(event.eventId().toString());
                rabbitTemplate.convertAndSend(EventTopics.EXCHANGE, EventTopics.USER_LOGGED_IN, event, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (confirm.isAck()) return;
                lastFailure = new IllegalStateException("Broker nack: " + confirm.getReason());
            } catch (Exception exception) {
                lastFailure = exception;
            }
            log.warn("Login event publish attempt failed; eventId={}, userId={}, attempt={}",
                    event.eventId(), userId, attempt, lastFailure);
        }
        failureCounter.increment();
        log.error("Login event publish failed; eventId={}, eventType={}, userId={}, attempts={}",
                event.eventId(), event.eventType(), userId, MAX_ATTEMPTS, lastFailure);
    }
}
