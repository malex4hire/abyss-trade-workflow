package com.abyssapplied.trade.service;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.repository.DeclarationRepository;
import com.abyssapplied.trade.repository.OutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeclarationService {
  private final DeclarationRepository declarations;
  private final OutboxRepository outbox;

  public DeclarationService(DeclarationRepository declarations, OutboxRepository outbox) {
    this.declarations = declarations;
    this.outbox = outbox;
  }

  /** New requests create new IDs. Business uniqueness remains undefined. */
  @Transactional
  public DeclarationResult createDraft(String owner, DeclarationData data) {
    UUID id = UUID.randomUUID();
    declarations.insert(id, owner, data);
    declarations.audit(id, "draft_created", "");
    return new DeclarationResult(id, DeclarationStatus.DRAFT);
  }

  @Transactional(readOnly = true)
  public List<TradeDeclaration> list(String owner) {
    return declarations.list(owner);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public TradeDeclaration get(String owner, UUID id) {
    var declaration = declarations.find(owner, id);
    return declaration.withAudit(declarations.auditHistory(id));
  }

  /** A row lock serializes submit requests for one declaration ID. */
  @Transactional
  public SubmissionResult submit(String owner, UUID id) {
    var current = declarations.lock(owner, id);
    if (current.status() != DeclarationStatus.DRAFT) {
      return new SubmissionResult(new DeclarationResult(id, current.status()), false);
    }
    declarations.updateStatus(id, DeclarationStatus.SUBMITTED, "");
    outbox.insert(new SubmissionEvent(UUID.randomUUID(), id));
    declarations.audit(id, "submitted", "");
    return new SubmissionResult(new DeclarationResult(id, DeclarationStatus.SUBMITTED), true);
  }
}
