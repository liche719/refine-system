package com.achobeta.refine.ai.conversation.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("refine.ai.memory")
public record ChatMemoryProperties(
        @DefaultValue("20") int maxMessages,
        @DefaultValue("24h") Duration idleTtl) {

    public ChatMemoryProperties {
        if (maxMessages < 2) {
            throw new IllegalArgumentException("refine.ai.memory.max-messages must be at least 2");
        }
        if (idleTtl == null || idleTtl.isNegative() || idleTtl.isZero()) {
            throw new IllegalArgumentException("refine.ai.memory.idle-ttl must be positive");
        }
    }
}
