package com.gokarting.domain.model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * TimeSlot domain entity.
 * The {@code bookedCount} is derived at query time — we don't store it here
 * to avoid stale reads. Capacity enforcement lives in {@code BookSlotService}.
 */
public class TimeSlot {

    private final UUID id;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int capacity;
    private final boolean active;

    public TimeSlot(UUID id, LocalTime startTime, LocalTime endTime,
                    int capacity, boolean active) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.active = active;
    }

    public boolean hasCapacity(int currentBookedCount) {
        return currentBookedCount < capacity;
    }

    public int remainingSlots(int currentBookedCount) {
        return Math.max(0, capacity - currentBookedCount);
    }

    public UUID getId()           { return id; }
    public LocalTime getStartTime(){ return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getCapacity()      { return capacity; }
    public boolean isActive()     { return active; }
}
