package com.gokarting.domain.port.out;

import java.util.UUID;

/** Port interface for recording business-level booking metrics. */
public interface BookingMetricsPort {

    void recordBookingCreated(UUID slotId);

    void recordBookingCancelled(UUID slotId);

    void recordSlotFull(UUID slotId);
}
