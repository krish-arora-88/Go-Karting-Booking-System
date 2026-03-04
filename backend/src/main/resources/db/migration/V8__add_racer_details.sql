-- V8: Add racer_count and racer_names to bookings.
-- racer_count: how many seats this booking consumes (1..capacity).
-- racer_names: JSON array of racer name strings.
-- Existing rows default to 1 racer with an empty names list.

ALTER TABLE bookings
    ADD COLUMN racer_count INT   NOT NULL DEFAULT 1,
    ADD COLUMN racer_names JSONB NOT NULL DEFAULT '[]'::jsonb;
