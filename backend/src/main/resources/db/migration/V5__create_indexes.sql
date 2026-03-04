-- V5: Indexes for hot query paths
-- All indexes created after data is in place (V6 seeds slots) to avoid
-- index maintenance overhead during bulk insert.

-- Booking lookups by slot + date (slot availability query)
-- Partial index on CONFIRMED only — cancelled bookings don't affect capacity
CREATE INDEX idx_bookings_slot_date_confirmed
    ON bookings (time_slot_id, booking_date)
    WHERE status = 'CONFIRMED';

-- Booking lookups by user (my bookings query)
CREATE INDEX idx_bookings_user_id
    ON bookings (user_id);

-- Outbox polling: only unpublished events, ordered by creation for FIFO delivery
CREATE INDEX idx_outbox_unpublished
    ON outbox_events (created_at ASC)
    WHERE published = FALSE;

-- Active time slots (default filter in slot listing)
CREATE INDEX idx_time_slots_active
    ON time_slots (start_time)
    WHERE is_active = TRUE;
