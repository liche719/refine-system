package com.achobeta.refine.common.observability;

import org.apache.skywalking.apm.meter.micrometer.SkywalkingMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkyWalkingMetricsConfigurationTest {
    @Test
    void createsRegistryForExistingMicrometerMetrics() {
        SkywalkingMeterRegistry registry = new SkyWalkingMetricsConfiguration().skywalkingMeterRegistry();
        try {
            registry.counter("refine.test.counter").increment();

            assertThat(registry.find("refine.test.counter").counter()).isNotNull();
        } finally {
            registry.close();
        }
    }
}
