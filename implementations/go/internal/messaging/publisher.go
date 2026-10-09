// Package messaging moves events between the transactional outbox and Kafka.
package messaging

import (
	"context"
	"log/slog"
	"time"

	"abyssapplied/trade-demo/internal/store"
	"github.com/segmentio/kafka-go"
)

const (
	SubmissionTopic = "trade.submitted"
	DeadLetterTopic = "trade.deadletter"
)

func RunPublisher(ctx context.Context, repository *store.Store, brokers []string) {
	writer := &kafka.Writer{
		Addr:                   kafka.TCP(brokers...),
		Topic:                  SubmissionTopic,
		Balancer:               &kafka.Hash{},
		RequiredAcks:           kafka.RequireAll,
		AllowAutoTopicCreation: true,
		WriteTimeout:           5 * time.Second,
	}
	defer writer.Close()

	send := func(ctx context.Context, message store.OutboxMessage) error {
		sendCtx, cancel := context.WithTimeout(ctx, 8*time.Second)
		defer cancel()
		err := writer.WriteMessages(sendCtx, kafka.Message{
			Key:   []byte(message.DeclarationID),
			Value: message.Payload,
		})
		if err == nil {
			slog.Info("broker acknowledged event", "eventId", message.EventID, "declarationId", message.DeclarationID)
		}
		return err
	}

	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if _, err := repository.PublishNext(ctx, send); err != nil {
				slog.Error("publish retry pending", "error", err)
			}
		}
	}
}
