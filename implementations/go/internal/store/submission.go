package store

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	"abyssapplied/trade-demo/internal/identifier"
	"abyssapplied/trade-demo/internal/trade"
	"github.com/jackc/pgx/v5"
)

// Submit serializes requests for ONE declaration ID. New drafts with identical
// business data have different IDs and remain independently submittable.
func (s *Store) Submit(ctx context.Context, owner, declarationID string) (trade.Status, bool, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return trade.Status{}, false, err
	}
	defer rollback(tx)

	var current trade.Status
	err = tx.QueryRow(ctx, `
        SELECT id::text, status FROM declarations
        WHERE id::text = $1 AND owner_id = $2
        FOR UPDATE
    `, declarationID, owner).Scan(&current.ID, &current.Status)
	if errors.Is(err, pgx.ErrNoRows) {
		return trade.Status{}, false, ErrNotFound
	}
	if err != nil {
		return trade.Status{}, false, err
	}

	// A simultaneous request waits for the row lock, then observes the status
	// committed by the first request and returns without creating another event.
	if current.Status != trade.Draft {
		return current, false, nil
	}
	event := trade.SubmissionEvent{EventID: identifier.New(), DeclarationID: current.ID}
	payload, err := json.Marshal(event)
	if err != nil {
		return trade.Status{}, false, err
	}

	_, err = tx.Exec(ctx, `UPDATE declarations SET status = $2 WHERE id = $1`, current.ID, trade.Submitted)
	if err != nil {
		return trade.Status{}, false, fmt.Errorf("update status: %w", err)
	}
	_, err = tx.Exec(ctx, `
        INSERT INTO outbox (event_id, declaration_id, payload)
        VALUES ($1, $2, $3)
    `, event.EventID, event.DeclarationID, payload)
	if err != nil {
		return trade.Status{}, false, fmt.Errorf("enqueue submission: %w", err)
	}
	if err := writeAudit(ctx, tx, current.ID, "submitted", ""); err != nil {
		return trade.Status{}, false, err
	}
	if err := tx.Commit(ctx); err != nil {
		return trade.Status{}, false, err
	}
	return trade.Status{ID: current.ID, Status: trade.Submitted}, true, nil
}
