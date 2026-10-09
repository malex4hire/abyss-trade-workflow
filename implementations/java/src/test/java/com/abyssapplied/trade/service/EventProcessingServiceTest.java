package com.abyssapplied.trade.service;

import static org.mockito.Mockito.*;

import com.abyssapplied.trade.domain.SubmissionEvent;
import com.abyssapplied.trade.repository.DeclarationRepository;
import com.abyssapplied.trade.repository.ProcessedEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventProcessingServiceTest {
  @Test
  void duplicateEventDoesNotReapplyTheDecision() {
    var processed = mock(ProcessedEventRepository.class);
    var declarations = mock(DeclarationRepository.class);
    var event = new SubmissionEvent(UUID.randomUUID(), UUID.randomUUID());
    when(processed.insertIfNew(event.eventId())).thenReturn(false);
    new EventProcessingService(
            processed, declarations, mock(com.abyssapplied.trade.repository.OutboxRepository.class))
        .process(event);
    verifyNoInteractions(declarations);
  }

  @Test
  void decisionAlsoWritesCompletionToOutbox() {
    var processed = mock(ProcessedEventRepository.class);
    var declarations = mock(DeclarationRepository.class);
    var outbox = mock(com.abyssapplied.trade.repository.OutboxRepository.class);
    var event = new SubmissionEvent(UUID.randomUUID(), UUID.randomUUID());
    when(processed.insertIfNew(event.eventId())).thenReturn(true);
    var data =
        new com.abyssapplied.trade.domain.DeclarationData(
            "Demo", "CA", "847130", 100, "USD", "REF");
    when(declarations.lockForProcessing(event.declarationId()))
        .thenReturn(
            new com.abyssapplied.trade.domain.TradeDeclaration(
                event.declarationId(),
                com.abyssapplied.trade.domain.DeclarationStatus.SUBMITTED,
                "",
                data,
                java.time.Instant.now(),
                null));
    new EventProcessingService(processed, declarations, outbox).process(event);
    verify(outbox)
        .insertCompletion(
            event.declarationId(), com.abyssapplied.trade.domain.TradeRules.decide(data).status());
  }
}
