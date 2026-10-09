package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

type OutboxMessage struct {
	EventID       string
	DeclarationID string
	Payload       []byte
}

// PublishNext keeps the row locked through broker acknowledgment. A crash after
// Kafka acknowledges but before commit can redeliver the same event ID.
func (s *Store) PublishNext(ctx context.Context, send func(context.Context, OutboxMessage) error) (bool, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return false, err
	}
	defer rollback(tx)

	var message OutboxMessage
	err = tx.QueryRow(ctx, `
        SELECT event_id::text, declaration_id::text, payload
        FROM outbox
        WHERE published_at IS NULL
        ORDER BY event_id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    `).Scan(&message.EventID, &message.DeclarationID, &message.Payload)
	if errors.Is(err, pgx.ErrNoRows) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	if err := send(ctx, message); err != nil {
		return false, fmt.Errorf("send outbox event: %w", err)
	}
	_, err = tx.Exec(ctx, `UPDATE outbox SET published_at = now() WHERE event_id = $1`, message.EventID)
	if err != nil {
		return false, err
	}
	if err := tx.Commit(ctx); err != nil {
		return false, err
	}
	return true, nil
}
