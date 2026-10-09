-- Initial schema. PostgreSQL runs this only when initializing a new volume.
-- No business uniqueness rule exists for shipment references in this demo.
CREATE TABLE IF NOT EXISTS declarations (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL,
    payload JSONB NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('draft', 'submitted', 'accepted', 'rejected')),
    reason TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit (
    sequence BIGSERIAL PRIMARY KEY,
    declaration_id UUID REFERENCES declarations (id),
    action TEXT NOT NULL,
    detail TEXT NOT NULL DEFAULT '',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A submission and its event enter the database in the same transaction.
CREATE TABLE IF NOT EXISTS outbox (
    event_id UUID PRIMARY KEY,
    declaration_id UUID REFERENCES declarations (id),
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ
);

-- The decision transaction inserts this ID to handle redelivery safely.
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS outbox_pending
    ON outbox (event_id)
    WHERE published_at IS NULL;
