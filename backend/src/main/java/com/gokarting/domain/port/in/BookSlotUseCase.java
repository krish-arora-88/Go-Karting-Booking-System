package com.gokarting.domain.port.in;

import com.gokarting.domain.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookSlotUseCase {

    record BookSlotCommand(UUID userId, UUID timeSlotId, LocalDate bookingDate,
                           String idempotencyKey, int racerCount, List<String> racerNames) {}

    Booking bookSlot(BookSlotCommand command);
}
