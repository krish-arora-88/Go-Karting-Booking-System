-- V7: Convert booking status column from PostgreSQL ENUM to VARCHAR for JPA compatibility.
-- Hibernate's @Enumerated(EnumType.STRING) sends status values as character varying,
-- which PostgreSQL rejects when the column is typed as a custom ENUM (no implicit cast).
--
-- To alter the column type we must first remove all ENUM-typed references:
--   1. The partial index whose predicate stores 'CONFIRMED' as booking_status ENUM
--   2. The column DEFAULT stored as 'CONFIRMED'::booking_status
-- Then we change the type and restore both with plain VARCHAR values.

DROP INDEX IF EXISTS idx_bookings_slot_date_confirmed;
DROP INDEX IF EXISTS idx_bookings_slot_date;

ALTER TABLE bookings ALTER COLUMN status DROP DEFAULT;
ALTER TABLE bookings ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE bookings ALTER COLUMN status SET DEFAULT 'CONFIRMED';

CREATE INDEX idx_bookings_slot_date_confirmed ON bookings(time_slot_id, booking_date) WHERE status = 'CONFIRMED';
