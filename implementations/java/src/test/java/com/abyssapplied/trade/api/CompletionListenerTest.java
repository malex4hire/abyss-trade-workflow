package com.abyssapplied.trade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.messaging.CompletionListener;
import com.abyssapplied.trade.repository.JsonCodec;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompletionListenerTest {
  @Test
  void completionNotifiesOwnerAndReplicasHaveSeparateGroups() {
    var json = mock(JsonCodec.class);
    var subscriptions = mock(DeclarationSubscription.class);
    var event =
        new CompletionEvent(
            UUID.randomUUID(), UUID.randomUUID(), "owner", DeclarationStatus.ACCEPTED);
    when(json.decode("payload", CompletionEvent.class)).thenReturn(event);
    var listener = new CompletionListener(json, subscriptions);
    listener.consume("payload");
    verify(subscriptions).changed("owner");
    assertThat(listener.getGroupId())
        .isNotEqualTo(new CompletionListener(json, subscriptions).getGroupId());
  }
}
