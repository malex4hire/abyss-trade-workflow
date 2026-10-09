package com.abyssapplied.trade.domain;

import java.util.UUID;

/** The outbox retains this event ID across broker retries and redelivery. */
public record SubmissionEvent(UUID eventId, UUID declarationId) {
  public SubmissionEvent {
    if (eventId == null || declarationId == null) {
      throw new IllegalArgumentException("eventId and declarationId are required");
    }
  }
}
