// Package trade defines the workflow data and illustrative business rules.
package trade

import "time"

const (
	Draft     = "draft"
	Submitted = "submitted"
	Accepted  = "accepted"
	Rejected  = "rejected"
	DemoOwner = "demo-importer"
)

// Declaration is the submitted business data. Values use integer cents.
type Declaration struct {
	Importer           string `json:"importer"`
	OriginCountry      string `json:"originCountry"`
	CommodityCode      string `json:"commodityCode"`
	DeclaredValueCents int64  `json:"declaredValueCents"`
	Currency           string `json:"currency"`
	ShipmentReference  string `json:"shipmentReference"`
}

type Status struct {
	ID     string `json:"id"`
	Status string `json:"status"`
}

type Record struct {
	ID        string       `json:"id"`
	Status    string       `json:"status"`
	Reason    string       `json:"reason"`
	Data      Declaration  `json:"data"`
	CreatedAt time.Time    `json:"createdAt"`
	Audit     []AuditEntry `json:"audit,omitempty"`
}

type AuditEntry struct {
	Action string    `json:"action"`
	Detail string    `json:"detail"`
	At     time.Time `json:"at"`
}

// SubmissionEvent retains its ID through retries and Kafka redelivery.
type SubmissionEvent struct {
	EventID       string `json:"eventId"`
	DeclarationID string `json:"declarationId"`
}
