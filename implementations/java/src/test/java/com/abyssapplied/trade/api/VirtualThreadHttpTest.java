package com.abyssapplied.trade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "trade.demo-token=test-token",
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    })
class VirtualThreadHttpTest {
  @LocalServerPort int port;
  @MockitoBean JdbcTemplate jdbc;
  @MockitoBean DeclarationSubscription subscriptions;
  @MockitoBean com.abyssapplied.trade.messaging.CompletionListener completions;

  @Test
  void actualTomcatRequestRunsOnAVirtualThread() throws Exception {
    when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
    try (var client = HttpClient.newHttpClient()) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health"))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.headers().firstValue("X-Virtual-Thread")).contains("true");
    }
  }
}
