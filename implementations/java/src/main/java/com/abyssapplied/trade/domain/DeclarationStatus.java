package com.abyssapplied.trade.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum DeclarationStatus {
  DRAFT,
  SUBMITTED,
  ACCEPTED,
  REJECTED;

  @JsonValue
  public String wireValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static DeclarationStatus fromDatabase(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
