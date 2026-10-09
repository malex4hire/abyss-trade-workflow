package com.abyssapplied.trade.api;

import com.abyssapplied.trade.domain.*;
import com.abyssapplied.trade.service.DeclarationNotFoundException;
import com.abyssapplied.trade.service.DeclarationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/declarations")
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class DeclarationController {
  private final DeclarationService service;

  public DeclarationController(DeclarationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<DeclarationResult> create(
      @Valid @RequestBody DeclarationData data, Principal user) {
    return ResponseEntity.status(201).body(service.createDraft(user.getName(), data));
  }

  @GetMapping
  public List<TradeDeclaration> list(Principal user) {
    return service.list(user.getName());
  }

  @GetMapping("/{id}")
  public TradeDeclaration get(@PathVariable String id, Principal user) {
    return service.get(user.getName(), parseID(id));
  }

  @PostMapping("/{id}/submit")
  public ResponseEntity<DeclarationResult> submit(@PathVariable String id, Principal user) {
    var result = service.submit(user.getName(), parseID(id));
    return ResponseEntity.status(result.newlySubmitted() ? 202 : 200).body(result.declaration());
  }

  private UUID parseID(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException error) {
      throw new DeclarationNotFoundException();
    }
  }
}
