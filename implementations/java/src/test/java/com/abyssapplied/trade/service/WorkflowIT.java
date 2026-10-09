package com.abyssapplied.trade.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.repository.JsonCodec;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "trade.mode=test")
class WorkflowIT {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired DeclarationService declarations;
  @Autowired EventProcessingService processing;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonCodec json;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void concurrentSubmitsAndDuplicateEventsProduceOneDecision() throws Exception {
    String owner = "test-" + UUID.randomUUID();
    var data = new DeclarationData("Demo", "CA", "847130", 125000, "USD", "SAME-DATA");
    var first = declarations.createDraft(owner, data);
    var second = declarations.createDraft(owner, data);
    assertThat(second.id()).isNotEqualTo(first.id());

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var requests = new ArrayList<Callable<SubmissionResult>>();
      for (int i = 0; i < 8; i++) requests.add(() -> declarations.submit(owner, first.id()));
      int newSubmissions = 0;
      for (var future : executor.invokeAll(requests)) {
        if (future.get().newlySubmitted()) newSubmissions++;
      }
      assertThat(newSubmissions).isEqualTo(1);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE declaration_id = ?", Integer.class, first.id()))
        .isEqualTo(1);
    String payload =
        jdbc.queryForObject(
            "SELECT payload::text FROM outbox WHERE declaration_id = ?", String.class, first.id());
    var event = json.decode(payload, SubmissionEvent.class);
    processing.process(event);
    processing.process(event);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE declaration_id = ? AND topic = 'trade.completed'",
                Integer.class,
                first.id()))
        .isEqualTo(1);
    var result = declarations.get(owner, first.id());
    assertThat(result.status()).isEqualTo(DeclarationStatus.ACCEPTED);
    assertThat(result.audit())
        .extracting(AuditEntry::action)
        .containsExactly("draft_created", "submitted", "accepted");
    assertThat(declarations.get(owner, second.id()).status()).isEqualTo(DeclarationStatus.DRAFT);
  }
}
