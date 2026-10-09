package com.abyssapplied.trade.messaging;

import com.abyssapplied.trade.repository.OutboxMessage;
import com.abyssapplied.trade.service.OutboxService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trade.mode", havingValue = "publisher")
public class OutboxPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxService service;
  private final KafkaTemplate<String, String> kafka;

  public OutboxPublisher(OutboxService service, KafkaTemplate<String, String> kafka) {
    this.service = service;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelayString = "${trade.publish-delay-ms:1000}")
  public void publish() {
    try {
      service.publishNext(this::send);
    } catch (RuntimeException error) {
      LOG.warn("Publish retry pending", error);
    }
  }

  private void send(OutboxMessage message) {
    try {
      kafka
          .send(message.topic(), message.declarationId().toString(), message.payload())
          .get(8, TimeUnit.SECONDS);
      LOG.info(
          "Broker acknowledged eventId={} declarationId={}",
          message.eventId(),
          message.declarationId());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Publisher interrupted", error);
    } catch (Exception error) {
      throw new IllegalStateException("Broker did not acknowledge event", error);
    }
  }
}
