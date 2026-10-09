package messaging

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"time"

	"abyssapplied/trade-demo/internal/identifier"
	"abyssapplied/trade-demo/internal/store"
	"abyssapplied/trade-demo/internal/trade"
	"github.com/segmentio/kafka-go"
)

func decodeEvent(payload []byte) (trade.SubmissionEvent, error) {
	var event trade.SubmissionEvent
	if err := json.Unmarshal(payload, &event); err != nil {
		return event, err
	}
	if !identifier.Valid(event.EventID) || !identifier.Valid(event.DeclarationID) {
		return event, fmt.Errorf("event and declaration IDs must be UUIDs")
	}
	return event, nil
}

func RunWorker(ctx context.Context, repository *store.Store, brokers []string) {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:  brokers,
		Topic:    SubmissionTopic,
		GroupID:  "trade-validator",
		MinBytes: 1,
		MaxBytes: 1000000,
	})
	defer reader.Close()
	deadLetters := &kafka.Writer{
		Addr:                   kafka.TCP(brokers...),
		Topic:                  DeadLetterTopic,
		RequiredAcks:           kafka.RequireAll,
		AllowAutoTopicCreation: true,
	}
	defer deadLetters.Close()

	for ctx.Err() == nil {
		message, err := reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				return
			}
			slog.Error("fetch event", "error", err)
			if !waitForRetry(ctx) {
				return
			}
			continue
		}
		event, decodeErr := decodeEvent(message.Value)
		// Finish this message before fetching another. Committing a later
		// offset could otherwise skip an earlier failed message in a partition.
		for ctx.Err() == nil {
			if decodeErr != nil {
				err = deadLetters.WriteMessages(ctx, kafka.Message{Key: message.Key, Value: message.Value})
			} else {
				err = repository.Process(ctx, event)
			}
			if err == nil {
				// Successful processing includes the duplicate-event no-op.
				err = reader.CommitMessages(ctx, message)
			}
			if err == nil {
				slog.Info("event acknowledged", "eventId", event.EventID, "declarationId", event.DeclarationID, "deadLetter", decodeErr != nil)
				break
			}
			slog.Error("processing retry; offset not committed", "error", err)
			if !waitForRetry(ctx) {
				return
			}
		}
	}
}

func waitForRetry(ctx context.Context) bool {
	timer := time.NewTimer(time.Second)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return false
	case <-timer.C:
		return true
	}
}
