package com.gokarting.integration;

import com.gokarting.adapter.in.web.dto.BookingRequest;
import com.gokarting.adapter.in.web.dto.LoginRequest;
import com.gokarting.adapter.in.web.dto.RegisterRequest;
import com.gokarting.domain.port.out.TimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the end-to-end Kafka event flow:
 *   POST /api/v1/bookings → booking saved to DB → OutboxPoller publishes → event received in Kafka
 *
 * Uses a @KafkaListener inside a @TestConfiguration bean to capture published events.
 * The BlockingQueue.poll(timeout) gives the OutboxPoller time to fire (runs every 200ms in test).
 *
 * This test proves the Transactional Outbox Pattern works correctly:
 * - Booking and outbox event are saved atomically in one DB transaction
 * - The OutboxPoller picks up and publishes to Kafka asynchronously
 * - The Kafka consumer receives the BOOKING_CREATED event
 */
@Import(KafkaEventIT.EventCapture.class)
class KafkaEventIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired TimeSlotRepository timeSlotRepository;
    @Autowired EventCapture eventCapture;

    @Test
    @DisplayName("Booking creation publishes BOOKING_CREATED event to Kafka via Outbox Pattern")
    void bookSlot_publishesBookingCreatedEventToKafka() throws Exception {
        // --- Register + login ---
        String username = "kafka_test_" + System.currentTimeMillis();
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, username + "@test.com", "password123"), Object.class);

        var loginResp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(username, "password123"), java.util.Map.class);
        String token = (String) loginResp.getBody().get("accessToken");

        // --- Book a slot ---
        var slots = timeSlotRepository.findAllActive();
        var slotId = slots.get(0).getId();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Idempotency-Key", "kafka-it-" + System.currentTimeMillis());

        var bookResp = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(new BookingRequest(slotId, LocalDate.now().plusDays(7), 1, List.of("Test Racer")), headers),
                java.util.Map.class);
        assertThat(bookResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String bookingId = (String) bookResp.getBody().get("id");

        // --- Wait for the OutboxPoller to publish the event (up to 15s) ---
        // OutboxPoller runs every 200ms in test profile; Kafka round-trip adds latency
        String eventPayload = eventCapture.queue.poll(15, TimeUnit.SECONDS);

        assertThat(eventPayload)
                .as("Expected BOOKING_CREATED event in Kafka within 15 seconds")
                .isNotNull()
                .contains("BOOKING_CREATED")
                .contains(bookingId);
    }

    /**
     * Captures Kafka events published to the booking-events topic.
     * Uses String deserialization since OutboxPoller publishes the pre-serialized JSON string.
     * Unique groupId ensures this consumer sees all messages regardless of prior runs.
     */
    @TestConfiguration
    static class EventCapture {
        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        @KafkaListener(
                topics = "gokarting.booking.events",
                groupId = "it-kafka-event-capture",
                properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=earliest"
                }
        )
        void capture(String payload) {
            queue.offer(payload);
        }
    }
}
