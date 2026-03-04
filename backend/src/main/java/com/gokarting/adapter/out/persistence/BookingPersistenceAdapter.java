package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.BookingEntity;
import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.BookingStatus;
import com.gokarting.domain.port.out.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingPersistenceAdapter implements BookingRepository {

    private final BookingJpaRepository jpa;

    @Override
    public Booking save(Booking booking) {
        return jpa.save(BookingEntity.fromDomain(booking)).toDomain();
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jpa.findById(id).map(BookingEntity::toDomain);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String key) {
        return jpa.findByIdempotencyKey(key).map(BookingEntity::toDomain);
    }

    @Override
    public List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status) {
        return jpa.findByUserIdAndStatus(userId, status).stream()
                .map(BookingEntity::toDomain)
                .toList();
    }

    @Override
    public int sumRacerCountByTimeSlotIdAndDateAndStatus(UUID timeSlotId, LocalDate date, BookingStatus status) {
        return jpa.sumRacerCountByTimeSlotIdAndDateAndStatus(timeSlotId, date, status);
    }
}
