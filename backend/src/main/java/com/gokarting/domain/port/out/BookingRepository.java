package com.gokarting.domain.port.out;

import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port — implemented by the JPA persistence adapter. */
public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByIdempotencyKey(String key);

    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    int sumRacerCountByTimeSlotIdAndDateAndStatus(UUID timeSlotId, LocalDate date, BookingStatus status);
}
