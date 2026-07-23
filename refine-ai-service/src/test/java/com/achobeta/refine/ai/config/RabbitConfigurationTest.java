package com.achobeta.refine.ai.config;

import com.achobeta.refine.contracts.event.EventTopics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigurationTest {
    @Test
    void activityQueueDeadLettersAfterListenerRetriesAreExhausted() {
        RabbitConfiguration configuration = new RabbitConfiguration();
        var queue = configuration.activityQueue();

        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", EventTopics.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfiguration.ACTIVITY_DLQ);
        assertThat(queue.isDurable()).isTrue();
        assertThat(configuration.loginQueue().getArguments())
                .containsEntry("x-dead-letter-exchange", EventTopics.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitConfiguration.LOGIN_DLQ);
        assertThat(configuration.activityDeadLetterQueue().isDurable()).isTrue();
        assertThat(configuration.loginDeadLetterQueue().isDurable()).isTrue();
        assertThat(configuration.activityBinding(configuration.domainEventsExchange(), queue).getRoutingKey())
                .isEqualTo(EventTopics.LEARNING_ACTIVITY_RECORDED);
        assertThat(configuration.loginBinding(configuration.domainEventsExchange(), configuration.loginQueue())
                .getRoutingKey()).isEqualTo(EventTopics.USER_LOGGED_IN);
    }

    @Test
    void listenerRetryIsFiniteAndExhaustionDoesNotRequeue() throws Exception {
        var propertySource = new YamlPropertySourceLoader()
                .load("ai", new ClassPathResource("application.yml")).getFirst();

        assertThat(propertySource.getProperty("spring.rabbitmq.listener.simple.retry.enabled")).isEqualTo(true);
        assertThat(propertySource.getProperty("spring.rabbitmq.listener.simple.retry.max-attempts")).isEqualTo(3);
        assertThat(propertySource.getProperty("spring.rabbitmq.listener.simple.default-requeue-rejected"))
                .isEqualTo(false);
    }
}
