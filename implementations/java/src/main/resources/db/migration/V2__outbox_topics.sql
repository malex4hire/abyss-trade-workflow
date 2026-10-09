ALTER TABLE outbox ADD COLUMN topic TEXT NOT NULL DEFAULT 'trade.submitted';
