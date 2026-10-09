package com.abyssapplied.trade.domain;

import java.util.UUID;

public record CompletionEvent(
    UUID eventId, UUID declarationId, String ownerId, DeclarationStatus status) {}
