package com.abyssapplied.trade.messaging;

import com.abyssapplied.trade.domain.SubmissionEvent;
import com.abyssapplied.trade.repository.JsonCodec;
import com.abyssapplied.trade.service.EventProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trade.mode", havingValue = "worker")
public class SubmissionListener {
  private static final Logger LOG = LoggerFactory.getLogger(SubmissionListener.class);
  private final JsonCodec json;
  private final EventProcessingService service;

  public SubmissionListener(JsonCodec json, EventProcessingService service) {
    this.json = json;
    this.service = service;
  }

  @KafkaListener(topics = Topics.SUBMITTED, groupId = "trade-validator-java")
  public void consume(String payload) {
    SubmissionEvent event;
    try {
      event = json.decode(payload, SubmissionEvent.class);
      if (event == null) throw new IllegalArgumentException("Submission event is required");
    } catch (RuntimeException error) {
      throw new MalformedEventException(error);
    }
    // This call crosses a Spring transactional proxy. It returns after DB commit.
    // RECORD acknowledgment then commits the Kafka offset. A crash can redeliver.
    service.process(event);
    LOG.info("Processed eventId={} declarationId={}", event.eventId(), event.declarationId());
  }
}
