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

// CreateDraft intentionally creates a new ID for each call. Business uniqueness
// and create-request idempotency remain requirements decisions.
func (s *Store) CreateDraft(ctx context.Context, owner string, data trade.Declaration) (trade.Status, error) {
	declarationID := identifier.New()
	payload, err := json.Marshal(data)
	if err != nil {
		return trade.Status{}, err
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return trade.Status{}, err
	}
	defer rollback(tx)

	_, err = tx.Exec(ctx, `
        INSERT INTO declarations (id, owner_id, payload, status)
        VALUES ($1, $2, $3, $4)
    `, declarationID, owner, payload, trade.Draft)
	if err != nil {
		return trade.Status{}, fmt.Errorf("insert draft: %w", err)
	}
	if err := writeAudit(ctx, tx, declarationID, "draft_created", ""); err != nil {
		return trade.Status{}, fmt.Errorf("audit draft: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return trade.Status{}, err
	}
	return trade.Status{ID: declarationID, Status: trade.Draft}, nil
}

func (s *Store) List(ctx context.Context, owner string) ([]trade.Record, error) {
	rows, err := s.pool.Query(ctx, `
        SELECT id::text, status, reason, payload, created_at
        FROM declarations
        WHERE owner_id = $1
        ORDER BY created_at DESC
        LIMIT 100
    `, owner)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	records := make([]trade.Record, 0)
	for rows.Next() {
		record, err := scanRecord(rows)
		if err != nil {
			return nil, err
		}
		records = append(records, record)
	}
	return records, rows.Err()
}

func (s *Store) Get(ctx context.Context, owner, declarationID string) (trade.Record, error) {
	// One query provides a consistent snapshot of status and audit history.
	row := s.pool.QueryRow(ctx, `
        SELECT d.id::text, d.status, d.reason, d.payload, d.created_at,
            COALESCE((
                SELECT jsonb_agg(
                    jsonb_build_object('action', action, 'detail', detail, 'at', occurred_at)
                    ORDER BY sequence
                )
                FROM audit WHERE declaration_id = d.id
            ), '[]'::jsonb)
        FROM declarations d
        WHERE d.id::text = $1 AND d.owner_id = $2
    `, declarationID, owner)

	var record trade.Record
	var payload, auditJSON []byte
	err := row.Scan(&record.ID, &record.Status, &record.Reason, &payload, &record.CreatedAt, &auditJSON)
	if errors.Is(err, pgx.ErrNoRows) {
		return trade.Record{}, ErrNotFound
	}
	if err != nil {
		return trade.Record{}, err
	}
	if err := json.Unmarshal(payload, &record.Data); err != nil {
		return trade.Record{}, err
	}
	if err := json.Unmarshal(auditJSON, &record.Audit); err != nil {
		return trade.Record{}, err
	}
	return record, nil
}

func scanRecord(row pgx.Row) (trade.Record, error) {
	var record trade.Record
	var payload []byte
	if err := row.Scan(&record.ID, &record.Status, &record.Reason, &payload, &record.CreatedAt); err != nil {
		return trade.Record{}, err
	}
	if err := json.Unmarshal(payload, &record.Data); err != nil {
		return trade.Record{}, err
	}
	return record, nil
}
