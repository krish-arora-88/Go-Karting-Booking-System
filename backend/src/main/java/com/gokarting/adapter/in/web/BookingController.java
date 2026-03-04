package com.gokarting.adapter.in.web;

import com.gokarting.adapter.in.web.dto.BookingRequest;
import com.gokarting.adapter.in.web.dto.BookingResponse;
import com.gokarting.adapter.in.web.dto.TimeSlotResponse;
import com.gokarting.domain.port.in.BookSlotUseCase;
import com.gokarting.domain.port.in.CancelBookingUseCase;
import com.gokarting.domain.port.in.GetTimeSlotsUseCase;
import com.gokarting.domain.port.out.TimeSlotRepository;
import com.gokarting.domain.port.out.UserRepository;
import com.gokarting.infrastructure.sse.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "View time slots and manage bookings")
public class BookingController {

    private final BookSlotUseCase bookSlotUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final GetTimeSlotsUseCase getTimeSlotsUseCase;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping(value = "/slots/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream for real-time slot availability",
               description = "Sends a 'slot-update' event when availability changes for the date. " +
                             "No auth required — EventSource cannot set headers.")
    @ApiResponse(responseCode = "200", description = "SSE stream opened")
    public SseEmitter streamSlots(
            @Parameter(description = "Date to watch", example = "2024-06-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return sseEmitterRegistry.createEmitter(date);
    }

    @GetMapping("/slots")
    @Operation(summary = "List available time slots for a given date",
               description = "Returns all active slots with live availability counts.")
    @ApiResponse(responseCode = "200", description = "Slot list returned")
    public List<TimeSlotResponse> getSlots(
            @Parameter(description = "Date to query (defaults to today)", example = "2024-06-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return getTimeSlotsUseCase.getAvailableSlots(queryDate).stream()
                .map(TimeSlotResponse::from)
                .toList();
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a time slot",
               description = "Books the specified slot on the given date. " +
                             "Supply X-Idempotency-Key header for safe retries — " +
                             "duplicate requests with the same key return the original booking.")
    @ApiResponse(responseCode = "201", description = "Booking confirmed")
    @ApiResponse(responseCode = "409", description = "Slot full or user already has a booking for this slot")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public BookingResponse book(
            @AuthenticationPrincipal String username,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequest req) {

        UUID userId = userRepository.findByUsername(username)
                .orElseThrow().getId();

        var booking = bookSlotUseCase.bookSlot(new BookSlotUseCase.BookSlotCommand(
                userId, req.timeSlotId(), req.bookingDate(), idempotencyKey,
                req.racerCount(), req.racerNames()));

        var slot = timeSlotRepository.findById(booking.getTimeSlotId()).orElseThrow();
        return BookingResponse.from(booking, slot, username);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get current user's active bookings")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal String username) {
        UUID userId = userRepository.findByUsername(username).orElseThrow().getId();
        return getTimeSlotsUseCase.getUserBookings(userId).stream()
                .map(b -> {
                    var slot = timeSlotRepository.findById(b.getTimeSlotId()).orElseThrow();
                    return BookingResponse.from(b, slot, username);
                })
                .toList();
    }

    @DeleteMapping("/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a booking",
               description = "Users can only cancel their own bookings.")
    @ApiResponse(responseCode = "204", description = "Booking cancelled")
    @ApiResponse(responseCode = "403", description = "Cannot cancel another user's booking")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    public void cancel(
            @AuthenticationPrincipal String username,
            @PathVariable UUID bookingId) {

        UUID userId = userRepository.findByUsername(username).orElseThrow().getId();
        cancelBookingUseCase.cancel(new CancelBookingUseCase.CancelCommand(bookingId, userId));
    }
}
