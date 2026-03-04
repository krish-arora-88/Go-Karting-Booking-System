package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.OutboxEventEntity;
import com.gokarting.domain.model.OutboxEvent;
import com.gokarting.domain.port.out.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxRepository {

    private final OutboxJpaRepository jpa;

    @Override
    public void save(OutboxEvent event) {
        jpa.save(OutboxEventEntity.builder()
                .aggregateId(event.aggregateId())
                .eventType(event.eventType())
                .topic(event.topic())
                .payload(event.payload())
                .published(event.published())
                .build());
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return jpa.findUnpublished(limit).stream()
                .map(e -> new OutboxEvent(e.getId(), e.getAggregateId(), e.getEventType(),
                        e.getTopic(), e.getPayload(), e.isPublished(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setPublished(true);
            jpa.save(e);
        });
    }
}
