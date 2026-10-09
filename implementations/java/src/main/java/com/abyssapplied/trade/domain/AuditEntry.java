package com.abyssapplied.trade.domain;

import java.time.Instant;

public record AuditEntry(String action, String detail, Instant at) {}
