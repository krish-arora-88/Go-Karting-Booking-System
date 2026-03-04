package com.gokarting.adapter.in.web.dto;

import com.gokarting.domain.port.in.GetTimeSlotsUseCase;

import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotResponse(
        UUID id,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int remaining,
        boolean available
) {
    public static TimeSlotResponse from(GetTimeSlotsUseCase.SlotWithAvailability s) {
        return new TimeSlotResponse(
                s.slot().getId(),
                s.slot().getStartTime(),
                s.slot().getEndTime(),
                s.slot().getCapacity(),
                s.remaining(),
                s.remaining() > 0
        );
    }
}
