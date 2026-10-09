package store

import (
	"context"
	"encoding/json"
	"fmt"

	"abyssapplied/trade-demo/internal/trade"
)

// Process commits event deduplication, the decision, and audit together.
// The Kafka consumer must commit its offset only AFTER this method succeeds.
func (s *Store) Process(ctx context.Context, event trade.SubmissionEvent) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer rollback(tx)

	inserted, err := tx.Exec(ctx, `
        INSERT INTO processed_events (event_id) VALUES ($1)
        ON CONFLICT DO NOTHING
    `, event.EventID)
	if err != nil {
		return err
	}
	if inserted.RowsAffected() == 0 {
		return tx.Commit(ctx)
	}

	var payload []byte
	var currentStatus string
	err = tx.QueryRow(ctx, `
        SELECT payload, status FROM declarations WHERE id = $1 FOR UPDATE
    `, event.DeclarationID).Scan(&payload, &currentStatus)
	if err != nil {
		return fmt.Errorf("load declaration for processing: %w", err)
	}
	if currentStatus != trade.Submitted {
		return tx.Commit(ctx)
	}

	var data trade.Declaration
	if err := json.Unmarshal(payload, &data); err != nil {
		return err
	}
	decision, reason := trade.Decide(data)
	_, err = tx.Exec(ctx, `
        UPDATE declarations SET status = $2, reason = $3 WHERE id = $1
    `, event.DeclarationID, decision, reason)
	if err != nil {
		return err
	}
	if err := writeAudit(ctx, tx, event.DeclarationID, decision, reason); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
