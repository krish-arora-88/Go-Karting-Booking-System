package com.gokarting.domain.port.out;

import com.gokarting.domain.model.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    void save(OutboxEvent event);

    List<OutboxEvent> findUnpublished(int limit);

    void markPublished(UUID id);
}
