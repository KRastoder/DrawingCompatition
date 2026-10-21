-- Migration V4: Add stored_blobs table for local storage
CREATE TABLE IF NOT EXISTS stored_blobs (
    id BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(36) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stored_blobs_storage_key ON stored_blobs (storage_key);