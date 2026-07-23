package com.achobeta.refine.ai.config;

import com.achobeta.refine.contracts.event.EventTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    public static final String LOGIN_QUEUE = "refine.ai.identity.user.logged-in.v1";
    public static final String ACTIVITY_QUEUE = "refine.ai.learning.activity.recorded.v1";
    public static final String LOGIN_DLQ = LOGIN_QUEUE + ".dlq";
    public static final String ACTIVITY_DLQ = ACTIVITY_QUEUE + ".dlq";

    @Bean
    TopicExchange domainEventsExchange() {
        return new TopicExchange(EventTopics.EXCHANGE, true, false);
    }

    @Bean
    TopicExchange domainEventsDeadLetterExchange() {
        return new TopicExchange(EventTopics.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue loginQueue() {
        return eventQueue(LOGIN_QUEUE, LOGIN_DLQ);
    }

    @Bean
    Queue activityQueue() {
        return eventQueue(ACTIVITY_QUEUE, ACTIVITY_DLQ);
    }

    @Bean
    Queue loginDeadLetterQueue() {
        return QueueBuilder.durable(LOGIN_DLQ).build();
    }

    @Bean
    Queue activityDeadLetterQueue() {
        return QueueBuilder.durable(ACTIVITY_DLQ).build();
    }

    @Bean
    Binding loginBinding(TopicExchange domainEventsExchange, Queue loginQueue) {
        return BindingBuilder.bind(loginQueue).to(domainEventsExchange).with(EventTopics.USER_LOGGED_IN);
    }

    @Bean
    Binding activityBinding(TopicExchange domainEventsExchange, Queue activityQueue) {
        return BindingBuilder.bind(activityQueue).to(domainEventsExchange)
                .with(EventTopics.LEARNING_ACTIVITY_RECORDED);
    }

    @Bean
    Binding loginDeadLetterBinding(TopicExchange domainEventsDeadLetterExchange, Queue loginDeadLetterQueue) {
        return BindingBuilder.bind(loginDeadLetterQueue).to(domainEventsDeadLetterExchange).with(LOGIN_DLQ);
    }

    @Bean
    Binding activityDeadLetterBinding(TopicExchange domainEventsDeadLetterExchange, Queue activityDeadLetterQueue) {
        return BindingBuilder.bind(activityDeadLetterQueue).to(domainEventsDeadLetterExchange).with(ACTIVITY_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue eventQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(EventTopics.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }
}
