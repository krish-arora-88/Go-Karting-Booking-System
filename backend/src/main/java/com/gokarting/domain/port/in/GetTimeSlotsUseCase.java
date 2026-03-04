package com.gokarting.domain.port.in;

import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetTimeSlotsUseCase {

    record SlotWithAvailability(TimeSlot slot, int bookedCount, int remaining) {}

    List<SlotWithAvailability> getAvailableSlots(LocalDate date);

    List<Booking> getUserBookings(UUID userId);
}
