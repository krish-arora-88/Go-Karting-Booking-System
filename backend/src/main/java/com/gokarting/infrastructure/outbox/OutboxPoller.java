package com.gokarting.infrastructure.outbox;

import com.gokarting.domain.port.out.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox_events table for unpublished events and sends them to Kafka.
 *
 * This is the "relay" step of the Transactional Outbox Pattern:
 *   - Events were written to outbox_events in the same DB transaction as the booking
 *   - This poller runs every 500ms, reads unpublished events, and publishes them
 *   - On success: marks event as published
 *   - On Kafka failure: leaves event unpublished (will retry on next poll)
 *
 * Trade-off: at-least-once delivery (event may be sent twice if app crashes after
 * Kafka send but before marking published). Consumers must handle duplicates.
 */
@Component
@Profile("!no-kafka")
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.outbox.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    @Transactional
    public void pollAndPublish() {
        var events = outboxRepository.findUnpublished(batchSize);
        if (events.isEmpty()) return;

        log.debug("Outbox poll: {} events to publish", events.size());

        for (var event : events) {
            try {
                kafkaTemplate.send(event.topic(), event.aggregateId().toString(), event.payload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event id={} type={}",
                                        event.id(), event.eventType(), ex);
                            } else {
                                outboxRepository.markPublished(event.id());
                                log.debug("Published outbox event id={} type={} to topic={}",
                                        event.id(), event.eventType(), event.topic());
                            }
                        });
            } catch (Exception e) {
                log.error("Outbox publish error for event id={}", event.id(), e);
            }
        }
    }
}
