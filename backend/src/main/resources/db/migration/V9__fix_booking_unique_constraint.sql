-- V9: Replace full unique constraint with partial index on CONFIRMED bookings only.
-- This allows a user to re-book a slot after cancelling their previous booking.

ALTER TABLE bookings DROP CONSTRAINT uq_bookings_user_slot_date;

CREATE UNIQUE INDEX uq_bookings_user_slot_date
    ON bookings (user_id, time_slot_id, booking_date)
    WHERE status = 'CONFIRMED';
