package com.abyssapplied.trade.messaging;

public class MalformedEventException extends RuntimeException {
  public MalformedEventException(Throwable cause) {
    super("Invalid submission event", cause);
  }
}
