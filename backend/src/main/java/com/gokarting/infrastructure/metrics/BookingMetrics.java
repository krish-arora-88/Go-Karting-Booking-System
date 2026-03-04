package com.gokarting.infrastructure.metrics;

import com.gokarting.domain.port.out.BookingMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom business metrics — these are what distinguish senior engineers:
 * not just "request count" but metrics that answer business questions.
 *
 * Exposed at /actuator/prometheus, scrapeable by Prometheus,
 * visualized in Grafana dashboards committed with the repo.
 */
@Component
@Slf4j
public class BookingMetrics implements BookingMetricsPort {

    private final MeterRegistry registry;

    // Pre-registered counters for hot paths (avoids map lookup on every request)
    private final Counter bookingsCreated;
    private final Counter bookingsCancelled;
    private final Counter slotsFullRejections;

    // Per-slot timers cached to avoid unbounded metric cardinality
    private final ConcurrentHashMap<String, Timer> bookingDurationTimers = new ConcurrentHashMap<>();

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.bookingsCreated = Counter.builder("bookings.created.total")
                .description("Total number of bookings successfully created")
                .register(registry);

        this.bookingsCancelled = Counter.builder("bookings.cancelled.total")
                .description("Total number of bookings cancelled")
                .register(registry);

        this.slotsFullRejections = Counter.builder("bookings.slot_full.total")
                .description("Booking attempts rejected because slot was at capacity")
                .register(registry);
    }

    public void recordBookingCreated(UUID slotId) {
        bookingsCreated.increment();
        // Tag with hour-of-slot to identify peak demand windows
        Counter.builder("bookings.created.by_slot")
                .tag("slot_id", slotId.toString().substring(0, 8))  // prefix only — avoid cardinality explosion
                .description("Bookings per slot (sampled)")
                .register(registry)
                .increment();
    }

    public void recordBookingCancelled(UUID slotId) {
        bookingsCancelled.increment();
    }

    public void recordSlotFull(UUID slotId) {
        slotsFullRejections.increment();
    }

    /** Returns a timer for measuring end-to-end booking request duration. */
    public Timer bookingRequestTimer() {
        return Timer.builder("bookings.request.duration")
                .description("End-to-end booking request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()   // enables histogram_quantile() in Prometheus
                .register(registry);
    }
}
