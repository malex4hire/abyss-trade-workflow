package com.abyssapplied.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trade")
public record TradeProperties(String mode, String demoToken) {}
