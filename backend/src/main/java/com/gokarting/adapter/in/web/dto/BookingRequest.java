package com.gokarting.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingRequest(
        @NotNull @Schema(description = "UUID of the time slot to book")
        UUID timeSlotId,

        @NotNull @Schema(description = "Date to book the slot for", example = "2024-06-15")
        LocalDate bookingDate,

        @Min(1) @Max(20) @Schema(description = "Number of racers (seats) to book", example = "2")
        int racerCount,

        @NotNull @Size(min = 1, max = 20)
        @Schema(description = "Name of each racer — length must equal racerCount")
        List<@NotBlank String> racerNames
) {}
