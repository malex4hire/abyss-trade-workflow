package com.abyssapplied.trade.repository;

import com.abyssapplied.trade.domain.DeclarationStatus;
import com.abyssapplied.trade.domain.SubmissionEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
  private final JdbcTemplate jdbc;
  private final JsonCodec json;

  public OutboxRepository(JdbcTemplate jdbc, JsonCodec json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public void insert(SubmissionEvent event) {
    jdbc.update(
        """
            INSERT INTO outbox (event_id, declaration_id, payload)
            VALUES (?, ?, CAST(? AS jsonb))
            """,
        event.eventId(),
        event.declarationId(),
        json.encode(event));
  }

  public void insertCompletion(UUID declarationId, DeclarationStatus status) {
    String owner =
        jdbc.queryForObject(
            "SELECT owner_id FROM declarations WHERE id = ?", String.class, declarationId);
    var event =
        new com.abyssapplied.trade.domain.CompletionEvent(
            java.util.UUID.randomUUID(), declarationId, owner, status);
    jdbc.update(
        "INSERT INTO outbox (event_id, declaration_id, payload, topic) VALUES (?, ?, CAST(? AS jsonb), ?)",
        event.eventId(),
        declarationId,
        json.encode(event),
        "trade.completed");
  }

  public Optional<OutboxMessage> lockNextPending() {
    var messages =
        jdbc.query(
            """
            SELECT event_id, declaration_id, payload, topic FROM outbox
            WHERE published_at IS NULL ORDER BY event_id
            LIMIT 1 FOR UPDATE SKIP LOCKED
            """,
            (rs, row) ->
                new OutboxMessage(
                    rs.getObject("event_id", UUID.class),
                    rs.getObject("declaration_id", UUID.class),
                    rs.getString("payload"),
                    rs.getString("topic")));
    return messages.stream().findFirst();
  }

  public void markPublished(UUID eventId) {
    jdbc.update("UPDATE outbox SET published_at = now() WHERE event_id = ?", eventId);
  }
}
