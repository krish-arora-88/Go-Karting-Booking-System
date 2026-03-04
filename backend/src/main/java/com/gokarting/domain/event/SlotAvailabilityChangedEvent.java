package com.gokarting.domain.event;

import java.time.LocalDate;

/**
 * Spring application event published (after transaction commit) whenever slot
 * availability changes — either a booking is created or cancelled.
 */
public record SlotAvailabilityChangedEvent(LocalDate date) {}
