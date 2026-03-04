package com.gokarting.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gokarting.domain.event.BookingEvent;
import com.gokarting.domain.event.SlotAvailabilityChangedEvent;
import com.gokarting.domain.exception.DuplicateBookingException;
import com.gokarting.domain.exception.ResourceNotFoundException;
import com.gokarting.domain.exception.SlotFullException;
import com.gokarting.domain.model.Booking;
import com.gokarting.domain.model.BookingStatus;
import com.gokarting.domain.model.OutboxEvent;
import com.gokarting.domain.port.in.BookSlotUseCase;
import com.gokarting.domain.port.out.BookingMetricsPort;
import com.gokarting.domain.port.out.BookingRepository;
import com.gokarting.domain.port.out.OutboxRepository;
import com.gokarting.domain.port.out.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookSlotService implements BookSlotUseCase {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final OutboxRepository outboxRepository;
    private final BookingMetricsPort metrics;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;

    /**
     * Books a slot atomically:
     * 1. Validates idempotency key (safe retry)
     * 2. Checks slot capacity with optimistic locking
     * 3. Creates Booking + OutboxEvent in a single transaction
     *
     * The OutboxPoller will pick up the event and publish to Kafka asynchronously.
     */
    @Transactional
    @Override
    public Booking bookSlot(BookSlotCommand command) {
        // --- Idempotency check: return existing booking if key already used ---
        if (command.idempotencyKey() != null) {
            var existing = bookingRepository.findByIdempotencyKey(command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent booking request for key={}", command.idempotencyKey());
                return existing.get();
            }
        }

        // --- Validate slot exists and is active ---
        var slot = timeSlotRepository.findById(command.timeSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time slot not found: " + command.timeSlotId()));

        if (!slot.isActive()) {
            throw new ResourceNotFoundException("Time slot is not active: " + command.timeSlotId());
        }

        // --- Capacity check (optimistic locking prevents race condition) ---
        int bookedSeats = bookingRepository.sumRacerCountByTimeSlotIdAndDateAndStatus(
                command.timeSlotId(), command.bookingDate(), BookingStatus.CONFIRMED);

        if (slot.remainingSlots(bookedSeats) < command.racerCount()) {
            metrics.recordSlotFull(command.timeSlotId());
            throw new SlotFullException(command.timeSlotId());
        }

        // --- Create booking ---
        var booking = new Booking(
                UUID.randomUUID(),
                command.userId(),
                command.timeSlotId(),
                command.bookingDate(),
                BookingStatus.CONFIRMED,
                command.idempotencyKey(),
                Instant.now(),
                command.racerCount(),
                command.racerNames()
        );

        try {
            Booking saved = bookingRepository.save(booking);

            // --- Write outbox event in SAME transaction ---
            writeOutboxEvent(saved, "BOOKING_CREATED");

            metrics.recordBookingCreated(command.timeSlotId());
            log.info("Booking created: id={} userId={} slotId={} date={}",
                    saved.getId(), saved.getUserId(), saved.getTimeSlotId(), saved.getBookingDate());

            // Notify SSE subscribers after transaction commits
            eventPublisher.publishEvent(new SlotAvailabilityChangedEvent(command.bookingDate()));

            return saved;

        } catch (ObjectOptimisticLockingFailureException e) {
            // Two concurrent requests hit the same slot version — retry or reject
            throw new SlotFullException(command.timeSlotId());
        } catch (DataIntegrityViolationException e) {
            // Unique constraint on (user_id, time_slot_id, booking_date) violated
            throw new DuplicateBookingException(
                    "User already has a booking for slot %s on %s"
                            .formatted(command.timeSlotId(), command.bookingDate()));
        }
    }

    private void writeOutboxEvent(Booking booking, String eventType) {
        var event = BookingEvent.of(
                eventType,
                booking.getId(),
                booking.getUserId(),
                booking.getTimeSlotId(),
                booking.getBookingDate(),
                booking.getStatus().name()
        );

        try {
            outboxRepository.save(OutboxEvent.create(
                    booking.getId(),
                    eventType,
                    bookingEventsTopic,
                    objectMapper.writeValueAsString(event)
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event for booking {}", booking.getId(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
