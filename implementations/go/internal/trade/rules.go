package trade

import (
	"fmt"
	"regexp"
	"strings"
)

var (
	countryPattern   = regexp.MustCompile(`^[A-Z]{2}$`)
	commodityPattern = regexp.MustCompile(`^[0-9]{6}$`)
)

// Validate checks syntax only. Reference-code validation requires client rules.
func Validate(data Declaration) error {
	if strings.TrimSpace(data.Importer) == "" || len(data.Importer) > 120 {
		return fmt.Errorf("importer is required (maximum 120 characters)")
	}
	if !countryPattern.MatchString(data.OriginCountry) {
		return fmt.Errorf("originCountry must contain two uppercase letters")
	}
	if !commodityPattern.MatchString(data.CommodityCode) {
		return fmt.Errorf("commodityCode must contain six digits")
	}
	if data.DeclaredValueCents <= 0 || data.DeclaredValueCents > 100000000000 {
		return fmt.Errorf("declaredValueCents must be between 1 and 100000000000")
	}
	if data.Currency != "USD" {
		return fmt.Errorf("this demo accepts USD only")
	}
	if strings.TrimSpace(data.ShipmentReference) == "" || len(data.ShipmentReference) > 80 {
		return fmt.Errorf("shipmentReference is required (maximum 80 characters)")
	}
	return nil
}

// Decide uses an invented threshold to demonstrate asynchronous processing.
// It is not an authoritative trade or customs determination.
func Decide(data Declaration) (string, string) {
	if data.DeclaredValueCents > 10000000 {
		return Rejected, "Demo rule: declared value exceeds the $100,000 limit"
	}
	return Accepted, "Demo rules passed; this is not a customs determination"
}
