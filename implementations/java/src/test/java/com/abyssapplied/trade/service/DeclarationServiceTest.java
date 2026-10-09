package com.abyssapplied.trade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.repository.DeclarationRepository;
import com.abyssapplied.trade.repository.OutboxRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeclarationServiceTest {
  private final DeclarationRepository declarations = mock(DeclarationRepository.class);
  private final OutboxRepository outbox = mock(OutboxRepository.class);
  private final DeclarationService service = new DeclarationService(declarations, outbox);

  @Test
  void repeatedSubmitOfAnExistingIdDoesNotEnqueueAnotherEvent() {
    UUID id = UUID.randomUUID();
    when(declarations.lock("owner", id)).thenReturn(record(id, DeclarationStatus.SUBMITTED));
    var result = service.submit("owner", id);
    assertThat(result.newlySubmitted()).isFalse();
    verifyNoInteractions(outbox);
    verify(declarations, never()).audit(any(), any(), any());
  }

  @Test
  void draftSubmissionUpdatesStatusAndEnqueuesAnEventForThatId() {
    UUID id = UUID.randomUUID();
    when(declarations.lock("owner", id)).thenReturn(record(id, DeclarationStatus.DRAFT));
    assertThat(service.submit("owner", id).newlySubmitted()).isTrue();
    verify(declarations).updateStatus(id, DeclarationStatus.SUBMITTED, "");
    verify(outbox).insert(argThat(event -> event.declarationId().equals(id)));
    verify(declarations).audit(id, "submitted", "");
  }

  private TradeDeclaration record(UUID id, DeclarationStatus status) {
    return new TradeDeclaration(
        id,
        status,
        "",
        new DeclarationData("Demo", "CA", "847130", 100, "USD", "ONE"),
        Instant.now(),
        null);
  }
}
