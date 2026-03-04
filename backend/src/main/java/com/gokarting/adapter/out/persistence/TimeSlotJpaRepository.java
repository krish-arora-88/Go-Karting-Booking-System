package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.TimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TimeSlotJpaRepository extends JpaRepository<TimeSlotEntity, UUID> {
    List<TimeSlotEntity> findAllByActiveTrue();
}
