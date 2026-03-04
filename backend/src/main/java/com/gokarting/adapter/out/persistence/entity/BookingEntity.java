package com.gokarting.adapter.out.persistence.entity;

import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_slot_id", nullable = false)
    private UUID timeSlotId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "racer_count", nullable = false)
    private int racerCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "racer_names", nullable = false, columnDefinition = "jsonb")
    private List<String> racerNames;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Booking toDomain() {
        return new Booking(id, userId, timeSlotId, bookingDate, status, idempotencyKey, createdAt,
                racerCount, racerNames != null ? racerNames : List.of());
    }

    public static BookingEntity fromDomain(Booking b) {
        return BookingEntity.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .timeSlotId(b.getTimeSlotId())
                .bookingDate(b.getBookingDate())
                .status(b.getStatus())
                .idempotencyKey(b.getIdempotencyKey())
                .racerCount(b.getRacerCount())
                .racerNames(b.getRacerNames())
                .createdAt(b.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }
}
