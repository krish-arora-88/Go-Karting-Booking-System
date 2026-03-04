package com.gokarting.adapter.out.persistence;

import com.gokarting.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.published = FALSE ORDER BY e.createdAt ASC LIMIT :limit")
    List<OutboxEventEntity> findUnpublished(@Param("limit") int limit);
}
