-- V1: Users table
-- Stores registered platform users with hashed passwords and roles.
-- UUID PKs avoid enumeration attacks and shard-friendly for future scaling.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- provides gen_random_uuid()

CREATE TABLE users (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username     VARCHAR(50) NOT NULL,
    email        VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_role    CHECK (role IN ('USER', 'ADMIN'))
);

COMMENT ON TABLE  users               IS 'Registered platform users';
COMMENT ON COLUMN users.role          IS 'RBAC role: USER or ADMIN';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, never plaintext';
