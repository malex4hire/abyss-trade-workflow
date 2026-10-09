package com.abyssapplied.trade.repository;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.service.DeclarationNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeclarationRepository {
  private final JdbcTemplate jdbc;
  private final JsonCodec json;

  public DeclarationRepository(JdbcTemplate jdbc, JsonCodec json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public void insert(UUID id, String owner, DeclarationData data) {
    jdbc.update(
        """
            INSERT INTO declarations (id, owner_id, payload, status)
            VALUES (?, ?, CAST(? AS jsonb), 'draft')
            """,
        id,
        owner,
        json.encode(data));
  }

  public List<TradeDeclaration> list(String owner) {
    return jdbc.query(
        """
            SELECT id, status, reason, payload, created_at
            FROM declarations WHERE owner_id = ?
            ORDER BY created_at DESC LIMIT 100
            """,
        this::mapRecord,
        owner);
  }

  public TradeDeclaration find(String owner, UUID id) {
    return single(
        jdbc.query(
            """
            SELECT id, status, reason, payload, created_at
            FROM declarations WHERE id = ? AND owner_id = ?
            """,
            this::mapRecord,
            id,
            owner));
  }

  public TradeDeclaration lock(String owner, UUID id) {
    return single(
        jdbc.query(
            """
            SELECT id, status, reason, payload, created_at
            FROM declarations WHERE id = ? AND owner_id = ? FOR UPDATE
            """,
            this::mapRecord,
            id,
            owner));
  }

  public TradeDeclaration lockForProcessing(UUID id) {
    return single(
        jdbc.query(
            """
            SELECT id, status, reason, payload, created_at
            FROM declarations WHERE id = ? FOR UPDATE
            """,
            this::mapRecord,
            id));
  }

  public void updateStatus(UUID id, DeclarationStatus status, String reason) {
    jdbc.update(
        "UPDATE declarations SET status = ?, reason = ? WHERE id = ?",
        status.wireValue(),
        reason,
        id);
  }

  public void audit(UUID id, String action, String detail) {
    jdbc.update(
        """
            INSERT INTO audit (declaration_id, action, detail) VALUES (?, ?, ?)
            """,
        id,
        action,
        detail);
  }

  public List<AuditEntry> auditHistory(UUID id) {
    return jdbc.query(
        """
            SELECT action, detail, occurred_at FROM audit
            WHERE declaration_id = ? ORDER BY sequence
            """,
        (rs, row) ->
            new AuditEntry(
                rs.getString("action"),
                rs.getString("detail"),
                rs.getTimestamp("occurred_at").toInstant()),
        id);
  }

  private TradeDeclaration mapRecord(ResultSet rs, int row) throws SQLException {
    return new TradeDeclaration(
        rs.getObject("id", UUID.class),
        DeclarationStatus.fromDatabase(rs.getString("status")),
        rs.getString("reason"),
        json.decode(rs.getString("payload"), DeclarationData.class),
        rs.getTimestamp("created_at").toInstant(),
        null);
  }

  private TradeDeclaration single(List<TradeDeclaration> records) {
    if (records.isEmpty()) throw new DeclarationNotFoundException();
    return records.getFirst();
  }
}
