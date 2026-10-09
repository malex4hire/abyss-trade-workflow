// Package config reads and validates process settings in one place.
package config

import (
	"fmt"
	"net"
	"os"
	"strings"
)

type Config struct {
	Mode         string
	DatabaseURL  string
	KafkaBrokers []string
	DemoToken    string
	HTTPAddress  string
}

func Load() (Config, error) {
	cfg := Config{
		Mode:        value("MODE", "api"),
		DatabaseURL: value("DATABASE_URL", "postgres://trade:trade@localhost:5432/trade?sslmode=disable"),
		DemoToken:   os.Getenv("DEMO_TOKEN"),
		HTTPAddress: net.JoinHostPort("", value("PORT", "8080")),
	}
	for _, broker := range strings.Split(value("KAFKA_BROKERS", "localhost:9092"), ",") {
		if broker = strings.TrimSpace(broker); broker != "" {
			cfg.KafkaBrokers = append(cfg.KafkaBrokers, broker)
		}
	}
	switch cfg.Mode {
	case "api", "publisher", "worker":
	default:
		return Config{}, fmt.Errorf("unknown MODE %q: use api, publisher, or worker", cfg.Mode)
	}
	if cfg.Mode == "api" && cfg.DemoToken == "" {
		return Config{}, fmt.Errorf("DEMO_TOKEN is required for the API")
	}
	if len(cfg.KafkaBrokers) == 0 {
		return Config{}, fmt.Errorf("KAFKA_BROKERS must contain at least one broker")
	}
	return cfg, nil
}

func value(key, fallback string) string {
	if current := os.Getenv(key); current != "" {
		return current
	}
	return fallback
}
