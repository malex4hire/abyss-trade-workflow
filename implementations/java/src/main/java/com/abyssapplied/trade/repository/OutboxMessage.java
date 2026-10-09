package com.abyssapplied.trade.repository;

import java.util.UUID;

public record OutboxMessage(UUID eventId, UUID declarationId, String payload, String topic) {}
