package com.gokarting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Booking aggregate root — the central domain concept.
 * Business rules encoded here are enforced regardless of which adapter
 * (REST, Kafka, batch) triggers the operation.
 */
public class Booking {

    private final UUID id;
    private final UUID userId;
    private final UUID timeSlotId;
    private final LocalDate bookingDate;
    private BookingStatus status;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final int racerCount;
    private final List<String> racerNames;

    public Booking(UUID id, UUID userId, UUID timeSlotId, LocalDate bookingDate,
                   BookingStatus status, String idempotencyKey, Instant createdAt,
                   int racerCount, List<String> racerNames) {
        this.id = id;
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.bookingDate = bookingDate;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.racerCount = racerCount;
        this.racerNames = racerNames;
    }

    /** Business rule: only CONFIRMED bookings can be cancelled. */
    public void cancel() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot cancel booking %s — current status: %s".formatted(id, status));
        }
        this.status = BookingStatus.CANCELLED;
    }

    public boolean isConfirmed() { return status == BookingStatus.CONFIRMED; }

    public UUID getId()               { return id; }
    public UUID getUserId()           { return userId; }
    public UUID getTimeSlotId()       { return timeSlotId; }
    public LocalDate getBookingDate() { return bookingDate; }
    public BookingStatus getStatus()  { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt()     { return createdAt; }
    public int getRacerCount()        { return racerCount; }
    public List<String> getRacerNames() { return racerNames; }
}
