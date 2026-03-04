package com.gokarting.domain.port.out;

import com.gokarting.domain.model.TimeSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository {

    List<TimeSlot> findAllActive();

    Optional<TimeSlot> findById(UUID id);

    TimeSlot save(TimeSlot slot);
}
