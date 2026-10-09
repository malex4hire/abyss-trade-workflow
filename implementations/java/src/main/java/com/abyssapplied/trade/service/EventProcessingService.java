package com.abyssapplied.trade.service;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.repository.DeclarationRepository;
import com.abyssapplied.trade.repository.OutboxRepository;
import com.abyssapplied.trade.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventProcessingService {
  private final ProcessedEventRepository processed;
  private final DeclarationRepository declarations;
  private final OutboxRepository outbox;

  public EventProcessingService(
      ProcessedEventRepository processed,
      DeclarationRepository declarations,
      OutboxRepository outbox) {
    this.processed = processed;
    this.declarations = declarations;
    this.outbox = outbox;
  }

  /** The event ID, decision and audit commit together before Kafka acknowledgment. */
  @Transactional
  public void process(SubmissionEvent event) {
    if (!processed.insertIfNew(event.eventId())) return;
    var declaration = declarations.lockForProcessing(event.declarationId());
    if (declaration.status() != DeclarationStatus.SUBMITTED) return;
    var decision = TradeRules.decide(declaration.data());
    declarations.updateStatus(declaration.id(), decision.status(), decision.reason());
    declarations.audit(declaration.id(), decision.status().wireValue(), decision.reason());
    outbox.insertCompletion(declaration.id(), decision.status());
  }
}
