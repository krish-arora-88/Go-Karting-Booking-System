-- V6: Seed 24 daily time slots (12:00–23:30, every 30 minutes, capacity 10)
-- Idempotent: uses INSERT ... ON CONFLICT DO NOTHING via a unique constraint
-- on (start_time, end_time) would be cleaner, but since we rely on UUID PKs,
-- this migration is safe to run once (Flyway checksums prevent re-runs).

INSERT INTO time_slots (start_time, end_time, capacity, is_active) VALUES
    ('12:00', '12:30', 10, TRUE),
    ('12:30', '13:00', 10, TRUE),
    ('13:00', '13:30', 10, TRUE),
    ('13:30', '14:00', 10, TRUE),
    ('14:00', '14:30', 10, TRUE),
    ('14:30', '15:00', 10, TRUE),
    ('15:00', '15:30', 10, TRUE),
    ('15:30', '16:00', 10, TRUE),
    ('16:00', '16:30', 10, TRUE),
    ('16:30', '17:00', 10, TRUE),
    ('17:00', '17:30', 10, TRUE),
    ('17:30', '18:00', 10, TRUE),
    ('18:00', '18:30', 10, TRUE),
    ('18:30', '19:00', 10, TRUE),
    ('19:00', '19:30', 10, TRUE),
    ('19:30', '20:00', 10, TRUE),
    ('20:00', '20:30', 10, TRUE),
    ('20:30', '21:00', 10, TRUE),
    ('21:00', '21:30', 10, TRUE),
    ('21:30', '22:00', 10, TRUE),
    ('22:00', '22:30', 10, TRUE),
    ('22:30', '23:00', 10, TRUE),
    ('23:00', '23:30', 10, TRUE),
    ('23:30', '23:59', 10, TRUE);
