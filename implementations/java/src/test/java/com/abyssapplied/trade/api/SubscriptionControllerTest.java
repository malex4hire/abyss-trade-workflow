package com.abyssapplied.trade.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.abyssapplied.trade.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(controllers = SubscriptionController.class, properties = "trade.demo-token=test-token")
@Import(SecurityConfig.class)
class SubscriptionControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean DeclarationSubscription subscriptions;

  @Test
  void requiresBearerToken() throws Exception {
    mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
    verifyNoInteractions(subscriptions);
  }

  @Test
  void portalOriginCanOpenAuthenticatedStream() throws Exception {
    var emitter = new SseEmitter(0L);
    when(subscriptions.subscribe("demo-importer")).thenReturn(emitter);
    mvc.perform(
            get("/api/events")
                .header("Authorization", "Bearer test-token")
                .header("Origin", "http://localhost:13000"))
        .andExpect(status().isOk())
        .andExpect(request().asyncStarted());
    verify(subscriptions).subscribe("demo-importer");
    emitter.complete();
  }
}
