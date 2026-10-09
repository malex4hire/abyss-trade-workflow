//go:build integration

package store

import (
	"context"
	"encoding/json"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"abyssapplied/trade-demo/internal/trade"
)

func integrationStore(t *testing.T) (*Store, context.Context) {
	t.Helper()
	databaseURL := os.Getenv("TEST_DATABASE_URL")
	if databaseURL == "" {
		t.Fatal("TEST_DATABASE_URL must point to a test database")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	t.Cleanup(cancel)
	repository, err := Open(ctx, databaseURL)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(repository.Close)
	owner := "test-" + strings.ReplaceAll(t.Name(), "/", "-")
	// Delete only this test's records. Never truncate a shared database.
	t.Cleanup(func() {
		cleanupCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_, _ = repository.pool.Exec(cleanupCtx, `
            DELETE FROM processed_events WHERE event_id IN (
                SELECT event_id FROM outbox WHERE declaration_id IN (SELECT id FROM declarations WHERE owner_id = $1)
            )`, owner)
		_, _ = repository.pool.Exec(cleanupCtx, `DELETE FROM outbox WHERE declaration_id IN (SELECT id FROM declarations WHERE owner_id = $1)`, owner)
		_, _ = repository.pool.Exec(cleanupCtx, `DELETE FROM audit WHERE declaration_id IN (SELECT id FROM declarations WHERE owner_id = $1)`, owner)
		_, _ = repository.pool.Exec(cleanupCtx, `DELETE FROM declarations WHERE owner_id = $1`, owner)
	})
	return repository, ctx
}

func TestConcurrentSubmitAndDuplicateEvent(t *testing.T) {
	repository, ctx := integrationStore(t)
	owner := "test-" + t.Name()
	data := trade.Declaration{Importer: "Synthetic Importer", OriginCountry: "CA", CommodityCode: "847130", DeclaredValueCents: 125000, Currency: "USD", ShipmentReference: "SAME-DATA"}
	first, err := repository.CreateDraft(ctx, owner, data)
	if err != nil {
		t.Fatal(err)
	}
	second, err := repository.CreateDraft(ctx, owner, data)
	if err != nil {
		t.Fatal(err)
	}
	if first.ID == second.ID {
		t.Fatal("new draft calls unexpectedly reused an ID")
	}

	var newSubmissions atomic.Int32
	var wg sync.WaitGroup
	errors := make(chan error, 8)
	for i := 0; i < 8; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_, newlySubmitted, err := repository.Submit(ctx, owner, first.ID)
			if err != nil {
				errors <- err
				return
			}
			if newlySubmitted {
				newSubmissions.Add(1)
			}
		}()
	}
	wg.Wait()
	close(errors)
	for err := range errors {
		t.Fatal(err)
	}
	if newSubmissions.Load() != 1 {
		t.Fatalf("new submissions: %d", newSubmissions.Load())
	}

	var payload []byte
	if err := repository.pool.QueryRow(ctx, `SELECT payload FROM outbox WHERE declaration_id = $1`, first.ID).Scan(&payload); err != nil {
		t.Fatal(err)
	}
	var event trade.SubmissionEvent
	if err := json.Unmarshal(payload, &event); err != nil {
		t.Fatal(err)
	}
	// Simulate redelivery of the exact event ID against a real PostgreSQL transaction.
	for i := 0; i < 2; i++ {
		if err := repository.Process(ctx, event); err != nil {
			t.Fatal(err)
		}
	}
	record, err := repository.Get(ctx, owner, first.ID)
	if err != nil {
		t.Fatal(err)
	}
	if record.Status != trade.Accepted || len(record.Audit) != 3 {
		t.Fatalf("unexpected result: %+v", record)
	}
	if record.Audit[1].Action != trade.Submitted || record.Audit[2].Action != trade.Accepted {
		t.Fatal(record.Audit)
	}
	untouched, err := repository.Get(ctx, owner, second.ID)
	if err != nil {
		t.Fatal(err)
	}
	if untouched.Status != trade.Draft {
		t.Fatal("second draft changed unexpectedly")
	}
	var eventCount int
	if err := repository.pool.QueryRow(ctx, `SELECT count(*) FROM outbox WHERE declaration_id = $1`, first.ID).Scan(&eventCount); err != nil {
		t.Fatal(err)
	}
	if eventCount != 1 {
		t.Fatalf("outbox events: %d", eventCount)
	}
}
