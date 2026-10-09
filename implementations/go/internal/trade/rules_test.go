package trade

import "testing"

func validDeclaration() Declaration {
	return Declaration{
		Importer: "Demo Importer", OriginCountry: "CA", CommodityCode: "847130",
		DeclaredValueCents: 125000, Currency: "USD", ShipmentReference: "SHIP-001",
	}
}

func TestValidate(t *testing.T) {
	if err := Validate(validDeclaration()); err != nil {
		t.Fatal(err)
	}
	cases := []struct {
		name   string
		change func(*Declaration)
	}{
		{"negative amount", func(d *Declaration) { d.DeclaredValueCents = -1 }},
		{"unsupported currency", func(d *Declaration) { d.Currency = "EUR" }},
		{"invalid commodity", func(d *Declaration) { d.CommodityCode = "abc" }},
		{"missing importer", func(d *Declaration) { d.Importer = "" }},
		{"invalid country", func(d *Declaration) { d.OriginCountry = "canada" }},
		{"blank shipment", func(d *Declaration) { d.ShipmentReference = "  " }},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			data := validDeclaration()
			tc.change(&data)
			if Validate(data) == nil {
				t.Fatal("invalid declaration accepted")
			}
		})
	}
}

func TestDecisionBoundary(t *testing.T) {
	data := validDeclaration()
	data.DeclaredValueCents = 10000000
	status, _ := Decide(data)
	if status != Accepted {
		t.Fatalf("limit: got %s", status)
	}
	data.DeclaredValueCents++
	status, _ = Decide(data)
	if status != Rejected {
		t.Fatalf("over limit: got %s", status)
	}
}
