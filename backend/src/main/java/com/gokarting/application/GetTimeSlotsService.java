package com.gokarting.application;

import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.BookingStatus;
import com.gokarting.domain.model.TimeSlot;
import com.gokarting.domain.port.in.GetTimeSlotsUseCase;
import com.gokarting.domain.port.out.BookingRepository;
import com.gokarting.domain.port.out.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTimeSlotsService implements GetTimeSlotsUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    @Override
    public List<SlotWithAvailability> getAvailableSlots(LocalDate date) {
        List<TimeSlot> slots = timeSlotRepository.findAllActive();
        return slots.stream()
                .map(slot -> {
                    int booked = bookingRepository.sumRacerCountByTimeSlotIdAndDateAndStatus(
                            slot.getId(), date, BookingStatus.CONFIRMED);
                    return new SlotWithAvailability(slot, booked, slot.remainingSlots(booked));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Booking> getUserBookings(UUID userId) {
        return bookingRepository.findByUserIdAndStatus(userId, BookingStatus.CONFIRMED);
    }
}
