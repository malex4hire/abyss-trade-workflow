package com.abyssapplied.trade.repository;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonCodec {
  private final ObjectMapper mapper;

  public JsonCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String encode(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JacksonException error) {
      throw new IllegalStateException("Could not encode stored JSON", error);
    }
  }

  public <T> T decode(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JacksonException error) {
      throw new IllegalStateException("Could not decode stored JSON", error);
    }
  }
}
