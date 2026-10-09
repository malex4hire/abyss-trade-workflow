package com.abyssapplied.trade;

import com.abyssapplied.trade.config.TradeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(TradeProperties.class)
public class TradeApplication {
  public static void main(String[] args) {
    SpringApplication.run(TradeApplication.class, args);
  }
}
