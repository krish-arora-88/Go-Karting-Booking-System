package com.gokarting.domain.port.out;

import com.gokarting.domain.model.Booking;

/** Output port for publishing booking domain events.
 *  Implemented by the Kafka adapter; can be swapped for in-memory in tests. */
public interface BookingEventPublisher {

    void publishBookingCreated(Booking booking);

    void publishBookingCancelled(Booking booking);
}
