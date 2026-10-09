package httpapi

import (
	"encoding/json"
	"io"
	"net/http"

	"abyssapplied/trade-demo/internal/trade"
)

func (a *API) createDraft(w http.ResponseWriter, r *http.Request) {
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192))
	decoder.DisallowUnknownFields()
	var data trade.Declaration
	if err := decoder.Decode(&data); err != nil {
		writeError(w, http.StatusBadRequest, "invalid declaration JSON")
		return
	}
	var extra any
	if err := decoder.Decode(&extra); err != io.EOF {
		writeError(w, http.StatusBadRequest, "exactly one JSON object required")
		return
	}
	if err := trade.Validate(data); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	// The bearer token models one local demo owner, not production identity.
	status, err := a.repository.CreateDraft(r.Context(), trade.DemoOwner, data)
	if err != nil {
		repositoryError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, status)
}

func (a *API) listDeclarations(w http.ResponseWriter, r *http.Request) {
	records, err := a.repository.List(r.Context(), trade.DemoOwner)
	if err != nil {
		repositoryError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, records)
}

func (a *API) getDeclaration(w http.ResponseWriter, r *http.Request) {
	record, err := a.repository.Get(r.Context(), trade.DemoOwner, r.PathValue("id"))
	if err != nil {
		repositoryError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, record)
}

func (a *API) submitDeclaration(w http.ResponseWriter, r *http.Request) {
	status, newlySubmitted, err := a.repository.Submit(r.Context(), trade.DemoOwner, r.PathValue("id"))
	if err != nil {
		repositoryError(w, err)
		return
	}
	if newlySubmitted {
		writeJSON(w, http.StatusAccepted, status)
		return
	}
	writeJSON(w, http.StatusOK, status)
}
