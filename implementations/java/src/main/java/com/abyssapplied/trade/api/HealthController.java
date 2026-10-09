package com.abyssapplied.trade.api;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class HealthController {
  private final JdbcTemplate jdbc;

  public HealthController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    try {
      jdbc.queryForObject("SELECT 1", Integer.class);
      return ResponseEntity.ok(Map.of("status", "ok"));
    } catch (RuntimeException error) {
      return ResponseEntity.status(503).body(Map.of("status", "database unavailable"));
    }
  }
}
