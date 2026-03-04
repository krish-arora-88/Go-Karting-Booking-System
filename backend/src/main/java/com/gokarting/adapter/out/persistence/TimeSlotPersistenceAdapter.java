package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.TimeSlotEntity;
import com.gokarting.domain.model.TimeSlot;
import com.gokarting.domain.port.out.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TimeSlotPersistenceAdapter implements TimeSlotRepository {

    private final TimeSlotJpaRepository jpa;

    @Override
    public List<TimeSlot> findAllActive() {
        return jpa.findAllByActiveTrue().stream()
                .map(TimeSlotEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TimeSlot> findById(UUID id) {
        return jpa.findById(id).map(TimeSlotEntity::toDomain);
    }

    @Override
    public TimeSlot save(TimeSlot slot) {
        // For now, read-update is handled via JPA entity directly in BookSlotService
        throw new UnsupportedOperationException("Use JPA entity directly for version-aware updates");
    }
}
