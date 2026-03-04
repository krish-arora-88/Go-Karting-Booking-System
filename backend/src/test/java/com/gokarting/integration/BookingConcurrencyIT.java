package com.gokarting.integration;

import com.gokarting.adapter.in.web.dto.BookingRequest;
import com.gokarting.adapter.in.web.dto.LoginRequest;
import com.gokarting.adapter.in.web.dto.RegisterRequest;
import com.gokarting.domain.port.out.TimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test: verifies that optimistic locking prevents overbooking
 * when multiple users simultaneously book the last available slot.
 *
 * This is a critical correctness property for a booking system.
 * Without the @Version column in time_slots, this test would fail
 * intermittently due to race conditions.
 */
class BookingConcurrencyIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired TimeSlotRepository timeSlotRepository;

    @Test
    @DisplayName("Concurrent booking of 1-capacity slot: exactly 1 succeeds, rest get 409")
    void concurrentBookings_optimisticLock_preventsOverbooking() throws InterruptedException {
        int concurrentUsers = 15;
        var slot = timeSlotRepository.findAllActive().get(10);  // use slot with capacity=10
        var date = LocalDate.now().plusDays(30);

        // Pre-fill 9 of 10 spots so only 1 slot remains
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tokens.add(registerAndGetToken("fill_user_" + System.nanoTime()));
        }
        for (String token : tokens) {
            var headers = bearerHeaders(token);
            restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                    new HttpEntity<>(new BookingRequest(slot.getId(), date, 1, List.of("Racer")), headers),
                    Object.class);
        }

        // Now race concurrentUsers threads for the last 1 spot
        var executor = Executors.newFixedThreadPool(concurrentUsers);
        var latch = new CountDownLatch(concurrentUsers);
        var successCount = new AtomicInteger(0);
        var conflictCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentUsers; i++) {
            String token = registerAndGetToken("racer_" + System.nanoTime());
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();  // all threads start simultaneously

                    var headers = bearerHeaders(token);
                    var resp = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                            new HttpEntity<>(new BookingRequest(slot.getId(), date, 1, List.of("Racer")), headers),
                            Object.class);

                    if (resp.getStatusCode() == HttpStatus.CREATED) successCount.incrementAndGet();
                    else if (resp.getStatusCode() == HttpStatus.CONFLICT) conflictCount.incrementAndGet();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Critical assertion: exactly 1 booking created, rest correctly rejected
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(concurrentUsers - 1);
    }

    private String registerAndGetToken(String username) {
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, username + "@test.com", "pass123456"), Object.class);
        var resp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(username, "pass123456"), java.util.Map.class);
        return (String) resp.getBody().get("accessToken");
    }

    private HttpHeaders bearerHeaders(String token) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }
}
