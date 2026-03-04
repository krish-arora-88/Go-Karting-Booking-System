package com.gokarting.adapter.out.persistence.entity;

import com.gokarting.domain.model.TimeSlot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "time_slots")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int capacity;

    /**
     * Optimistic locking: JPA increments this on every UPDATE.
     * If two transactions read the same version and both try to update,
     * the second will get an OptimisticLockException, which we catch
     * in BookSlotService and translate to a 409 Conflict.
     */
    @Version
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public TimeSlot toDomain() {
        return new TimeSlot(id, startTime, endTime, capacity, active);
    }
}
