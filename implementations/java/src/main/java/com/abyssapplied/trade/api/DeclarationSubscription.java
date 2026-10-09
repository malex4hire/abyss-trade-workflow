package com.abyssapplied.trade.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Local connections receive notifications consumed by this API replica. */
@Component
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class DeclarationSubscription {
  private final ConcurrentHashMap<SseEmitter, String> subscribers = new ConcurrentHashMap<>();
  private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();

  public SseEmitter subscribe(String owner) {
    var emitter = new SseEmitter(0L);
    subscribers.put(emitter, owner);
    emitter.onCompletion(() -> subscribers.remove(emitter));
    emitter.onTimeout(() -> subscribers.remove(emitter));
    emitter.onError(error -> subscribers.remove(emitter));
    // Refresh durable state on every connection; SSE is not a durable event log.
    send(emitter, "reset");
    return emitter;
  }

  public void changed(String owner) {
    subscribers.forEach(
        (emitter, subscriber) -> {
          if (owner.equals(subscriber)) send(emitter, "changed");
        });
  }

  private void send(SseEmitter emitter, String event) {
    try {
      emitter.send(SseEmitter.event().name(event).data("{}"));
    } catch (IOException | IllegalStateException error) {
      subscribers.remove(emitter);
      emitter.complete();
    }
  }

  @PostConstruct
  void start() {
    heartbeat.scheduleAtFixedRate(
        () -> subscribers.keySet().forEach(emitter -> send(emitter, "heartbeat")),
        15,
        15,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  void stop() {
    heartbeat.shutdownNow();
    subscribers.keySet().forEach(SseEmitter::complete);
    subscribers.clear();
  }
}
