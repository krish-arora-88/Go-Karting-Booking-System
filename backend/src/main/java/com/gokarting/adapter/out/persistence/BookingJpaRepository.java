package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.BookingEntity;
import com.gokarting.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String key);

    List<BookingEntity> findByUserIdAndStatus(UUID userId, BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.racerCount), 0) FROM BookingEntity b " +
           "WHERE b.timeSlotId = :slotId AND b.bookingDate = :date AND b.status = :status")
    int sumRacerCountByTimeSlotIdAndDateAndStatus(
            @Param("slotId") UUID slotId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatus status);
}
