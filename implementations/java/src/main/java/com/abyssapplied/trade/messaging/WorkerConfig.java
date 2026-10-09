package com.abyssapplied.trade.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@org.springframework.context.annotation.Profile({"worker", "api"})
public class WorkerConfig {
  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
    var recoverer =
        new DeadLetterPublishingRecoverer(
            kafka,
            (record, error) ->
                new TopicPartition(
                    record.topic().equals(Topics.COMPLETED)
                        ? "trade.completed.deadletter"
                        : Topics.DEAD_LETTER,
                    -1));
    // Never treat failed dead-letter publication as successful recovery.
    recoverer.setFailIfSendResultIsError(true);
    var handler =
        new DefaultErrorHandler(
            recoverer, new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    handler.addNotRetryableExceptions(MalformedEventException.class);
    return handler;
  }
}
