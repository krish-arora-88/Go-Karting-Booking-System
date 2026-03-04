-- V4: Transactional Outbox table
-- Solves the dual-write problem: instead of writing to the DB and publishing
-- to Kafka in two separate operations (which can leave them out of sync on crash),
-- we write the event to this table in the SAME transaction as the booking.
-- A scheduled poller then reads unpublished events and sends them to Kafka.
--
-- This guarantees: if the booking is committed, the event will eventually be published.
-- If the app crashes before publishing, the event is still in the DB and will be
-- picked up on next startup.

CREATE TABLE outbox_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID        NOT NULL,           -- booking_id or user_id
    event_type   VARCHAR(100) NOT NULL,          -- e.g. BOOKING_CREATED
    topic        VARCHAR(255) NOT NULL,          -- Kafka topic
    payload      JSONB       NOT NULL,
    published    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE outbox_events IS 'Transactional outbox: events written here in same TX as domain changes, then polled to Kafka';
