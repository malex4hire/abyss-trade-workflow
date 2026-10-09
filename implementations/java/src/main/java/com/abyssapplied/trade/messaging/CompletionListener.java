package com.abyssapplied.trade.messaging;

import com.abyssapplied.trade.api.DeclarationSubscription;
import com.abyssapplied.trade.domain.CompletionEvent;
import com.abyssapplied.trade.repository.JsonCodec;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class CompletionListener {
  // A group per replica broadcasts each event to every replica's local SSE clients.
  // A shared group would deliver to only one replica and miss other browsers.
  private final String groupId = "trade-api-status-" + UUID.randomUUID();
  private final JsonCodec json;
  private final DeclarationSubscription subscriptions;

  public CompletionListener(JsonCodec json, DeclarationSubscription subscriptions) {
    this.json = json;
    this.subscriptions = subscriptions;
  }

  public String getGroupId() {
    return groupId;
  }

  @KafkaListener(
      topics = Topics.COMPLETED,
      groupId = "#{__listener.groupId}",
      properties = "auto.offset.reset=earliest")
  public void consume(String payload) {
    CompletionEvent event;
    try {
      event = json.decode(payload, CompletionEvent.class);
    } catch (RuntimeException error) {
      throw new MalformedEventException(error);
    }
    if (event == null
        || event.ownerId() == null
        || event.declarationId() == null
        || event.eventId() == null
        || event.status() == null)
      throw new MalformedEventException(new IllegalArgumentException("Invalid completion event"));
    // Repeated events only request a fresh snapshot; they never repeat a decision.
    subscriptions.changed(event.ownerId());
  }
}
