package httpapi

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"abyssapplied/trade-demo/internal/store"
	"abyssapplied/trade-demo/internal/trade"
)

type fakeRepository struct {
	createCalls    int
	newlySubmitted bool
}

func (f *fakeRepository) Ping(context.Context) error { return nil }
func (f *fakeRepository) CreateDraft(_ context.Context, owner string, data trade.Declaration) (trade.Status, error) {
	f.createCalls++
	return trade.Status{ID: "test-id", Status: trade.Draft}, nil
}
func (f *fakeRepository) List(context.Context, string) ([]trade.Record, error) {
	return []trade.Record{}, nil
}
func (f *fakeRepository) Get(context.Context, string, string) (trade.Record, error) {
	return trade.Record{}, store.ErrNotFound
}
func (f *fakeRepository) Submit(context.Context, string, string) (trade.Status, bool, error) {
	return trade.Status{ID: "test-id", Status: trade.Submitted}, f.newlySubmitted, nil
}

func TestDraftRequestValidation(t *testing.T) {
	const valid = `{"importer":"Demo","originCountry":"CA","commodityCode":"847130","declaredValueCents":100,"currency":"USD","shipmentReference":"ONE"}`
	cases := []struct {
		name, body, token string
		want              int
	}{
		{"valid", valid, "Bearer secret", http.StatusCreated},
		{"unauthorized", valid, "Bearer wrong", http.StatusUnauthorized},
		{"multiple JSON objects", valid + " {}", "Bearer secret", http.StatusBadRequest},
		{"unknown field", strings.TrimSuffix(valid, "}") + `,"extra":true}`, "Bearer secret", http.StatusBadRequest},
		{"invalid input", strings.Replace(valid, "847130", "abc", 1), "Bearer secret", http.StatusBadRequest},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			repo := &fakeRepository{}
			request := httptest.NewRequest(http.MethodPost, "/api/declarations", strings.NewReader(tc.body))
			request.Header.Set("Authorization", tc.token)
			response := httptest.NewRecorder()
			New(repo, "secret").ServeHTTP(response, request)
			if response.Code != tc.want {
				t.Fatalf("got %d: %s", response.Code, response.Body.String())
			}
			if tc.want != http.StatusCreated && repo.createCalls != 0 {
				t.Fatal("invalid request reached persistence")
			}
		})
	}
}

func TestSubmissionResponseDistinguishesFirstRequest(t *testing.T) {
	for _, newlySubmitted := range []bool{true, false} {
		repo := &fakeRepository{newlySubmitted: newlySubmitted}
		request := httptest.NewRequest(http.MethodPost, "/api/declarations/test-id/submit", nil)
		request.Header.Set("Authorization", "Bearer secret")
		response := httptest.NewRecorder()
		New(repo, "secret").ServeHTTP(response, request)
		want := http.StatusOK
		if newlySubmitted {
			want = http.StatusAccepted
		}
		if response.Code != want {
			t.Fatalf("got %d, want %d", response.Code, want)
		}
	}
}

func TestSwaggerPreflight(t *testing.T) {
	for _, origin := range []string{"http://localhost:18081", "http://127.0.0.1:18081", "https://untrusted.example"} {
		request := httptest.NewRequest(http.MethodOptions, "/api/declarations", nil)
		request.Header.Set("Origin", origin)
		request.Header.Set("Access-Control-Request-Method", "GET")
		request.Header.Set("Access-Control-Request-Headers", "Authorization")
		response := httptest.NewRecorder()
		New(&fakeRepository{}, "secret").ServeHTTP(response, request)
		if origin == "https://untrusted.example" {
			if response.Header().Get("Access-Control-Allow-Origin") != "" {
				t.Fatal("untrusted origin allowed")
			}
		} else if response.Code != http.StatusNoContent || response.Header().Get("Access-Control-Allow-Origin") != origin {
			t.Fatalf("preflight failed: %d %+v", response.Code, response.Header())
		}
	}
}
