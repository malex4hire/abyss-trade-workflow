package com.abyssapplied.trade.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.abyssapplied.trade.config.SecurityConfig;
import com.abyssapplied.trade.domain.DeclarationResult;
import com.abyssapplied.trade.domain.DeclarationStatus;
import com.abyssapplied.trade.service.DeclarationService;
import com.abyssapplied.trade.service.SubmissionResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DeclarationController.class, properties = "trade.demo-token=test-token")
@Import(SecurityConfig.class)
class DeclarationControllerTest {
  private static final String VALID =
      """
        {"importer":"Demo","originCountry":"CA","commodityCode":"847130",
         "declaredValueCents":125000,"currency":"USD","shipmentReference":"ONE"}
        """;
  @Autowired MockMvc mvc;
  @MockitoBean DeclarationService service;

  @Test
  void rejectsUnauthenticatedRequests() throws Exception {
    mvc.perform(get("/api/declarations")).andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void validatesBeforeCallingTheService() throws Exception {
    mvc.perform(
            post("/api/declarations")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("125000", "-1")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
    verifyNoInteractions(service);
  }

  @Test
  void createsADraftWithTheDemoPrincipal() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.createDraft(eq("demo-importer"), any()))
        .thenReturn(new DeclarationResult(id, DeclarationStatus.DRAFT));
    mvc.perform(
            post("/api/declarations")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("draft"));
  }

  @Test
  void returnsCurrentStatusForARepeatedSubmit() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.submit("demo-importer", id))
        .thenReturn(
            new SubmissionResult(new DeclarationResult(id, DeclarationStatus.SUBMITTED), false));
    mvc.perform(
            post("/api/declarations/" + id + "/submit")
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("submitted"));
  }

  @Test
  void invalidIdRetainsThe404Contract() throws Exception {
    mvc.perform(get("/api/declarations/not-a-uuid").header("Authorization", "Bearer test-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  void allowsSwaggerPreflightWithoutABearerToken() throws Exception {
    mvc.perform(
            options("/api/declarations")
                .header("Origin", "http://localhost:18081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:18081"));
  }
}
