-- V3: Bookings table
-- Records a user's reservation of a time slot on a specific date.
-- idempotency_key prevents duplicate submissions on client retry (e.g. network timeout).
-- The partial unique index on (time_slot_id, booking_date) only counts CONFIRMED bookings,
-- so a user can re-book after cancelling.

CREATE TYPE booking_status AS ENUM ('CONFIRMED', 'CANCELLED');

CREATE TABLE bookings (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES users(id)      ON DELETE RESTRICT,
    time_slot_id     UUID           NOT NULL REFERENCES time_slots(id) ON DELETE RESTRICT,
    booking_date     DATE           NOT NULL,
    status           booking_status NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key  VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    -- One booking per user per slot per day
    CONSTRAINT uq_bookings_user_slot_date UNIQUE (user_id, time_slot_id, booking_date),
    -- Idempotency: same key always returns same booking
    CONSTRAINT uq_bookings_idempotency_key UNIQUE (idempotency_key)
);

COMMENT ON TABLE  bookings                  IS 'User reservations for time slots';
COMMENT ON COLUMN bookings.idempotency_key  IS 'Client-supplied key for safe retry — same key returns existing booking';
COMMENT ON COLUMN bookings.status           IS 'CONFIRMED = active booking; CANCELLED = released back to pool';
