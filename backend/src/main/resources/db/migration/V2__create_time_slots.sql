-- V2: Time slots table
-- Represents bookable racing sessions. The `version` column enables
-- optimistic locking in JPA (@Version) to prevent double-booking under
-- concurrent load without requiring a distributed lock.

CREATE TABLE time_slots (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    capacity   INT         NOT NULL,
    version    INT         NOT NULL DEFAULT 0,   -- optimistic lock counter
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_time_slots_capacity  CHECK (capacity > 0),
    CONSTRAINT chk_time_slots_time_order CHECK (end_time > start_time)
);

COMMENT ON TABLE  time_slots         IS 'Available racing session slots';
COMMENT ON COLUMN time_slots.version IS 'JPA @Version field — incremented on every update to prevent lost updates';
