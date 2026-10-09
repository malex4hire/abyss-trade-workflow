package com.abyssapplied.trade.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TradeDeclaration(
    UUID id,
    DeclarationStatus status,
    String reason,
    DeclarationData data,
    Instant createdAt,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<AuditEntry> audit) {
  public TradeDeclaration withAudit(List<AuditEntry> entries) {
    return new TradeDeclaration(id, status, reason, data, createdAt, entries);
  }
}
