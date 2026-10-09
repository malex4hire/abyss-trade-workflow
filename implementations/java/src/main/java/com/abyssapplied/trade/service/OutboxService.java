package com.abyssapplied.trade.service;

import com.abyssapplied.trade.repository.OutboxMessage;
import com.abyssapplied.trade.repository.OutboxRepository;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
  private final OutboxRepository outbox;

  public OutboxService(OutboxRepository outbox) {
    this.outbox = outbox;
  }

  /** Hold the row lock until broker acknowledgment. A crash before commit can redeliver. */
  @Transactional
  public boolean publishNext(Consumer<OutboxMessage> send) {
    var pending = outbox.lockNextPending();
    if (pending.isEmpty()) return false;
    var message = pending.get();
    send.accept(message);
    outbox.markPublished(message.eventId());
    return true;
  }
}
