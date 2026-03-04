package com.gokarting.adapter.in.web.dto;

import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.TimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID timeSlotId,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate bookingDate,
        String status,
        Instant createdAt,
        String bookedBy,
        int racerCount,
        List<String> racerNames
) {
    public static BookingResponse from(Booking b, TimeSlot slot, String bookedBy) {
        return new BookingResponse(
                b.getId(),
                b.getTimeSlotId(),
                slot.getStartTime(),
                slot.getEndTime(),
                b.getBookingDate(),
                b.getStatus().name(),
                b.getCreatedAt(),
                bookedBy,
                b.getRacerCount(),
                b.getRacerNames()
        );
    }
}
