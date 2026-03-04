package com.gokarting.infrastructure.sse;

import com.gokarting.domain.event.SlotAvailabilityChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE emitters per booking date.
 *
 * When a booking is created or cancelled, BookSlotService/CancelBookingService
 * publishes a SlotAvailabilityChangedEvent. This registry listens for that event
 * AFTER the transaction commits (@TransactionalEventListener) and pushes a
 * lightweight 'slot-update' notification to all connected clients.
 *
 * Clients then re-fetch the /api/v1/slots endpoint to get fresh availability.
 * This separates the notification concern (infrastructure) from business logic.
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes

    private final Map<LocalDate, List<SseEmitter>> emittersByDate = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(LocalDate date) {
        var emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByDate.computeIfAbsent(date, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(date, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        log.debug("SSE client connected for date={}, total clients={}", date,
                emittersByDate.getOrDefault(date, List.of()).size());
        return emitter;
    }

    /**
     * Fires AFTER the booking transaction commits so clients re-fetching see
     * the updated availability immediately.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSlotChanged(SlotAvailabilityChangedEvent event) {
        var clients = emittersByDate.getOrDefault(event.date(), List.of());
        if (clients.isEmpty()) return;

        log.debug("Broadcasting slot-update to {} SSE clients for date={}", clients.size(), event.date());

        for (SseEmitter emitter : clients) {
            try {
                emitter.send(SseEmitter.event()
                        .name("slot-update")
                        .data("{\"date\":\"" + event.date() + "\"}"));
            } catch (IOException e) {
                removeEmitter(event.date(), emitter);
            }
        }
    }

    private void removeEmitter(LocalDate date, SseEmitter emitter) {
        var list = emittersByDate.get(date);
        if (list != null) list.remove(emitter);
    }
}
