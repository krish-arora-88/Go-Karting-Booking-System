package com.gokarting.domain.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** CloudEvents-inspired envelope for all booking domain events. */
public record BookingEvent(
        String eventId,
        String eventType,        // BOOKING_CREATED | BOOKING_CANCELLED
        String aggregateId,      // booking UUID
        Instant occurredAt,
        int version,
        Payload payload
) {
    public record Payload(
            String bookingId,
            String userId,
            String timeSlotId,
            LocalDate bookingDate,
            String status
    ) {}

    public static BookingEvent of(String eventType, UUID bookingId, UUID userId,
                                  UUID timeSlotId, LocalDate bookingDate, String status) {
        return new BookingEvent(
                UUID.randomUUID().toString(),
                eventType,
                bookingId.toString(),
                Instant.now(),
                1,
                new Payload(
                        bookingId.toString(),
                        userId.toString(),
                        timeSlotId.toString(),
                        bookingDate,
                        status
                )
        );
    }
}
