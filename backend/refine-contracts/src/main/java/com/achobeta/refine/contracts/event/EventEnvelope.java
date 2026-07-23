package com.achobeta.refine.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String userId,
        T payload) {

    public static <T> EventEnvelope<T> create(String eventType, String userId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, 1, Instant.now(), userId, payload);
    }
}
