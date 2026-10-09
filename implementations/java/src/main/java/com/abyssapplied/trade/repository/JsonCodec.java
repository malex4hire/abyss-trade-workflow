package com.abyssapplied.trade.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonCodec {
  private final ObjectMapper mapper;

  public JsonCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String encode(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Could not encode stored JSON", error);
    }
  }

  public <T> T decode(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Could not decode stored JSON", error);
    }
  }
}
