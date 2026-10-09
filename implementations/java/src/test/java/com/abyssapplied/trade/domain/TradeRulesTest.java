package com.abyssapplied.trade.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TradeRulesTest {
  @Test
  void acceptsTheDemoLimitAndRejectsOneCentOver() {
    assertThat(TradeRules.decide(data(10000000L)).status()).isEqualTo(DeclarationStatus.ACCEPTED);
    assertThat(TradeRules.decide(data(10000001L)).status()).isEqualTo(DeclarationStatus.REJECTED);
  }

  private DeclarationData data(long cents) {
    return new DeclarationData("Demo", "CA", "847130", cents, "USD", "ONE");
  }
}
