package com.abyssapplied.trade.api;

import java.security.Principal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class SubscriptionController {
  private final DeclarationSubscription subscriptions;

  public SubscriptionController(DeclarationSubscription subscriptions) {
    this.subscriptions = subscriptions;
  }

  @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(Principal user) {
    return subscriptions.subscribe(user.getName());
  }
}
