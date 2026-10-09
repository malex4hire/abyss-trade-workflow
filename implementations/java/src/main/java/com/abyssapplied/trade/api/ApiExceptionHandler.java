package com.abyssapplied.trade.api;

import com.abyssapplied.trade.service.DeclarationNotFoundException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(DeclarationNotFoundException.class)
  ResponseEntity<Map<String, String>> notFound() {
    return error(404, "declaration not found");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
    var field = exception.getBindingResult().getFieldErrors().getFirst();
    return error(400, field.getField() + " " + field.getDefaultMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, String>> invalidJson() {
    return error(400, "invalid declaration JSON");
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Map<String, String>> database(DataAccessException exception) {
    LOG.error("Database request failed", exception);
    return error(503, "could not complete database request");
  }

  private ResponseEntity<Map<String, String>> error(int status, String message) {
    return ResponseEntity.status(status).body(Map.of("error", message));
  }
}
