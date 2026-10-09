package com.abyssapplied.trade.domain;

/** Illustrative rule only. The client must define actual trade decisions. */
public final class TradeRules {
  private TradeRules() {}

  public static Decision decide(DeclarationData data) {
    if (data.declaredValueCents() > 10000000L) {
      return new Decision(
          DeclarationStatus.REJECTED, "Demo rule: declared value exceeds the $100,000 limit");
    }
    return new Decision(
        DeclarationStatus.ACCEPTED, "Demo rules passed; this is not a customs determination");
  }
}
