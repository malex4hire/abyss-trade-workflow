// Package httpapi handles HTTP contracts. It does not contain SQL or Kafka code.
package httpapi

import (
	"context"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"strings"
	"time"

	"abyssapplied/trade-demo/internal/identifier"
	"abyssapplied/trade-demo/internal/store"
	"abyssapplied/trade-demo/internal/trade"
)

// Repository defines only the persistence operations needed by the API.
type Repository interface {
	Ping(context.Context) error
	CreateDraft(context.Context, string, trade.Declaration) (trade.Status, error)
	List(context.Context, string) ([]trade.Record, error)
	Get(context.Context, string, string) (trade.Record, error)
	Submit(context.Context, string, string) (trade.Status, bool, error)
}

type API struct {
	repository Repository
	token      string
}

func New(repository Repository, token string) http.Handler {
	api := &API{repository: repository, token: token}
	routes := http.NewServeMux()
	routes.HandleFunc("GET /health", api.health)
	routes.HandleFunc("POST /api/declarations", api.createDraft)
	routes.HandleFunc("GET /api/declarations", api.listDeclarations)
	routes.HandleFunc("GET /api/declarations/{id}", api.getDeclaration)
	routes.HandleFunc("POST /api/declarations/{id}/submit", api.submitDeclaration)
	return allowLocalDocs(api.authenticate(routes))
}

func (a *API) authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestID := identifier.New()
		w.Header().Set("X-Request-ID", requestID)
		if strings.HasPrefix(r.URL.Path, "/api/") {
			expected := "Bearer " + a.token
			if subtle.ConstantTimeCompare([]byte(r.Header.Get("Authorization")), []byte(expected)) != 1 {
				writeError(w, http.StatusUnauthorized, "valid demo bearer token required")
				return
			}
		}
		slog.Info("request", "requestId", requestID, "method", r.Method, "path", r.URL.Path)
		next.ServeHTTP(w, r)
	})
}

func (a *API) health(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()
	if a.repository.Ping(ctx) != nil {
		http.Error(w, "database unavailable", http.StatusServiceUnavailable)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(value); err != nil {
		slog.Error("write response", "error", err)
	}
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

func repositoryError(w http.ResponseWriter, err error) {
	if errors.Is(err, store.ErrNotFound) {
		writeError(w, http.StatusNotFound, "declaration not found")
		return
	}
	slog.Error("repository request failed", "error", err)
	writeError(w, http.StatusServiceUnavailable, "could not complete database request")
}
