package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"abyssapplied/trade-demo/internal/config"
	"abyssapplied/trade-demo/internal/httpapi"
	"abyssapplied/trade-demo/internal/messaging"
	"abyssapplied/trade-demo/internal/store"
)

func main() {
	if err := run(); err != nil {
		slog.Error("application stopped", "error", err)
		os.Exit(1)
	}
}

func run() error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()
	repository, err := store.Open(ctx, cfg.DatabaseURL)
	if err != nil {
		return err
	}
	defer repository.Close()

	switch cfg.Mode {
	case "publisher":
		messaging.RunPublisher(ctx, repository, cfg.KafkaBrokers)
	case "worker":
		messaging.RunWorker(ctx, repository, cfg.KafkaBrokers)
	case "api":
		return serveAPI(ctx, cfg, repository)
	}
	return nil
}

func serveAPI(ctx context.Context, cfg config.Config, repository *store.Store) error {
	server := &http.Server{
		Addr:              cfg.HTTPAddress,
		Handler:           httpapi.New(repository, cfg.DemoToken),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}
	shutdownDone := make(chan struct{})
	go func() {
		defer close(shutdownDone)
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := server.Shutdown(shutdownCtx); err != nil {
			slog.Error("graceful shutdown", "error", err)
			_ = server.Close()
		}
	}()
	slog.Info("api started", "address", cfg.HTTPAddress)
	err := server.ListenAndServe()
	if errors.Is(err, http.ErrServerClosed) {
		<-shutdownDone
		return nil
	}
	return err
}
