package com.gokarting.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gokarting.domain.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes booking events and dispatches notifications.
 * In a real system this would call an email/SMS service.
 * Here it demonstrates the consumer pattern including:
 *   - JSON deserialization
 *   - Idempotency (event IDs logged for deduplication)
 *   - Consumer group offset management (via group-id in application.yml)
 *   - DLT routing (via KafkaConfig.errorHandler)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.booking-events}",
            groupId = "gokarting-notifications",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            var event = objectMapper.readValue(payload, BookingEvent.class);

            switch (event.eventType()) {
                case "BOOKING_CREATED"   -> sendBookingConfirmation(event);
                case "BOOKING_CANCELLED" -> sendCancellationNotification(event);
                default -> log.warn("Unknown event type: {}", event.eventType());
            }

            log.info("Processed {} eventId={} offset={} topic={}",
                    event.eventType(), event.eventId(), offset, topic);

        } catch (Exception e) {
            log.error("Failed to process booking event at offset={}", offset, e);
            throw new RuntimeException("Notification processing failed", e);
            // Spring Kafka will route to DLT after configured retries
        }
    }

    private void sendBookingConfirmation(BookingEvent event) {
        // TODO: integrate with SendGrid / AWS SES / Twilio
        log.info("[NOTIFICATION] Booking confirmed — bookingId={} userId={} slotId={} date={}",
                event.payload().bookingId(),
                event.payload().userId(),
                event.payload().timeSlotId(),
                event.payload().bookingDate());
    }

    private void sendCancellationNotification(BookingEvent event) {
        log.info("[NOTIFICATION] Booking cancelled — bookingId={} userId={}",
                event.payload().bookingId(),
                event.payload().userId());
    }
}
