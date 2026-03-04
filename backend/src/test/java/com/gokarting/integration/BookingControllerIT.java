package com.gokarting.integration;

import com.gokarting.adapter.in.web.dto.BookingRequest;
import com.gokarting.adapter.in.web.dto.LoginRequest;
import com.gokarting.adapter.in.web.dto.RegisterRequest;
import com.gokarting.domain.port.out.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end integration test using real PostgreSQL, Kafka, and Redis via Testcontainers.
 * Tests the complete booking flow: register → login → get slots → book → cancel.
 */
class BookingControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired TimeSlotRepository timeSlotRepository;

    private String accessToken;
    private String userId;

    @BeforeEach
    void setUp() {
        // Register and login a test user
        var registerReq = new RegisterRequest("testuser_" + System.currentTimeMillis(), "test@test.com", "password123");
        var registerResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Object.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var loginReq = new LoginRequest(registerReq.username(), "password123");
        var loginResp = restTemplate.postForEntity("/api/v1/auth/login", loginReq,
                java.util.Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        accessToken = (String) loginResp.getBody().get("accessToken");
    }

    @Test
    @DisplayName("GET /api/v1/slots returns 24 seeded time slots")
    void getSlots_returns24Slots() {
        var headers = authHeaders();
        var resp = restTemplate.exchange("/api/v1/slots", HttpMethod.GET,
                new HttpEntity<>(headers), Object[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(24);
    }

    @Test
    @DisplayName("POST /api/v1/bookings creates a booking and Flyway schema is applied")
    void bookSlot_createsBooking() {
        var slots = timeSlotRepository.findAllActive();
        assertThat(slots).isNotEmpty();

        var slotId = slots.get(0).getId();
        var req = new BookingRequest(slotId, LocalDate.now().plusDays(1), 1, List.of("Test Racer"));
        var headers = authHeaders();
        headers.set("X-Idempotency-Key", "test-idem-key-" + System.currentTimeMillis());

        var resp = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), java.util.Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsKey("id");
        assertThat(resp.getBody().get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Idempotency key returns same booking on duplicate request")
    void bookSlot_idempotencyKey_returnsSameBooking() {
        var slots = timeSlotRepository.findAllActive();
        var slotId = slots.get(1).getId();
        var req = new BookingRequest(slotId, LocalDate.now().plusDays(2), 1, List.of("Test Racer"));
        var key = "idem-" + System.currentTimeMillis();
        var headers = authHeaders();
        headers.set("X-Idempotency-Key", key);

        var first = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), java.util.Map.class);
        var second = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), java.util.Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bookings/{id} cancels a booking")
    void cancelBooking_returnsNoContent() {
        var slots = timeSlotRepository.findAllActive();
        var slotId = slots.get(2).getId();
        var req = new BookingRequest(slotId, LocalDate.now().plusDays(3), 1, List.of("Test Racer"));
        var headers = authHeaders();

        var bookResp = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), java.util.Map.class);
        String bookingId = (String) bookResp.getBody().get("id");

        var cancelResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private HttpHeaders authHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
