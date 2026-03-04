package com.gokarting.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gokarting.domain.event.BookingEvent;
import com.gokarting.domain.event.SlotAvailabilityChangedEvent;
import com.gokarting.domain.exception.ResourceNotFoundException;
import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.OutboxEvent;
import com.gokarting.domain.port.in.CancelBookingUseCase;
import com.gokarting.domain.port.out.BookingMetricsPort;
import com.gokarting.domain.port.out.BookingRepository;
import com.gokarting.domain.port.out.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelBookingService implements CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final OutboxRepository outboxRepository;
    private final BookingMetricsPort metrics;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;

    @Transactional
    @Override
    public void cancel(CancelCommand command) {
        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + command.bookingId()));

        // Authorization: users can only cancel their own bookings (admins can cancel any)
        if (!booking.getUserId().equals(command.requestingUserId())) {
            throw new AccessDeniedException("Cannot cancel another user's booking");
        }

        booking.cancel();  // enforces CONFIRMED → CANCELLED state transition
        bookingRepository.save(booking);

        writeOutboxEvent(booking);
        metrics.recordBookingCancelled(booking.getTimeSlotId());
        log.info("Booking cancelled: id={} userId={}", booking.getId(), booking.getUserId());

        // Notify SSE subscribers after transaction commits
        eventPublisher.publishEvent(new SlotAvailabilityChangedEvent(booking.getBookingDate()));
    }

    private void writeOutboxEvent(Booking booking) {
        var event = BookingEvent.of(
                "BOOKING_CANCELLED",
                booking.getId(),
                booking.getUserId(),
                booking.getTimeSlotId(),
                booking.getBookingDate(),
                booking.getStatus().name()
        );
        try {
            outboxRepository.save(OutboxEvent.create(
                    booking.getId(),
                    "BOOKING_CANCELLED",
                    bookingEventsTopic,
                    objectMapper.writeValueAsString(event)
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cancel outbox event for booking {}", booking.getId(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
