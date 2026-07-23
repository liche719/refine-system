package com.achobeta.refine.ai.analytics.messaging;

import com.achobeta.refine.ai.analytics.application.LearningAnalysisService;
import com.achobeta.refine.ai.config.RabbitConfiguration;
import com.achobeta.refine.contracts.event.EventEnvelope;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.achobeta.refine.contracts.event.UserLoggedInPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LearningEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(LearningEventConsumer.class);
    private final LearningAnalysisService service;

    public LearningEventConsumer(LearningAnalysisService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfiguration.ACTIVITY_QUEUE)
    public void onLearningActivity(EventEnvelope<LearningActivityPayload> event) {
        boolean consumed = service.consume(event);
        log.info("Learning activity event handled; eventId={}, userId={}, consumed={}",
                event.eventId(), event.userId(), consumed);
    }

    @RabbitListener(queues = RabbitConfiguration.LOGIN_QUEUE)
    public void onUserLoggedIn(EventEnvelope<UserLoggedInPayload> event) {
        boolean consumed = service.consumeLogin(event);
        log.info("User login event handled; eventId={}, userId={}, consumed={}",
                event.eventId(), event.userId(), consumed);
    }
}
