package com.abyssapplied.trade.service;

public class DeclarationNotFoundException extends RuntimeException {
  public DeclarationNotFoundException() {
    super("declaration not found");
  }
}
