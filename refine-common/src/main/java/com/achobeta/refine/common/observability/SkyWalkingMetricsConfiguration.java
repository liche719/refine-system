package com.achobeta.refine.common.observability;

import org.apache.skywalking.apm.meter.micrometer.SkywalkingMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SkyWalkingMetricsConfiguration {
    @Bean
    @ConditionalOnMissingBean(SkywalkingMeterRegistry.class)
    public SkywalkingMeterRegistry skywalkingMeterRegistry() {
        return new SkywalkingMeterRegistry();
    }
}
