package messaging

import "testing"

func TestMalformedEvents(t *testing.T) {
	for _, payload := range []string{`{`, `{}`, `{"eventId":"bad","declarationId":"bad"}`} {
		if _, err := decodeEvent([]byte(payload)); err == nil {
			t.Fatalf("accepted %q", payload)
		}
	}
	valid := `{"eventId":"12345678-1234-4234-8234-123456789012","declarationId":"87654321-1234-4234-8234-123456789012"}`
	if _, err := decodeEvent([]byte(valid)); err != nil {
		t.Fatal(err)
	}
}
