package com.achobeta.refine.learning.config;

import com.achobeta.refine.contracts.event.EventTopics;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    @Bean TopicExchange domainEventsExchange() { return new TopicExchange(EventTopics.EXCHANGE, true, false); }
    @Bean Jackson2JsonMessageConverter rabbitJsonConverter() { return new Jackson2JsonMessageConverter(); }
}
