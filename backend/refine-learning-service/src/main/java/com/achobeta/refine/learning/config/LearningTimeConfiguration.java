package com.achobeta.refine.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LearningTimeConfiguration {
    @Bean
    Clock learningClock() {
        return Clock.systemUTC();
    }
}
