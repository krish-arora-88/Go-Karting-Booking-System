package com.gokarting.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Domain model for an outbox event (no JPA annotations — persistence is adapter concern). */
public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String topic,
        String payload,
        boolean published,
        Instant createdAt
) {
    /** Factory for new (unsaved) events — id and createdAt are assigned by persistence layer. */
    public static OutboxEvent create(UUID aggregateId, String eventType, String topic, String payload) {
        return new OutboxEvent(null, aggregateId, eventType, topic, payload, false, null);
    }
}
