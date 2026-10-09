package com.abyssapplied.trade.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, TradeProperties properties, ObjectMapper mapper) throws Exception {
    if (properties.demoToken() == null || properties.demoToken().isBlank()) {
      throw new IllegalStateException("DEMO_TOKEN is required for the API");
    }
    return http.csrf(csrf -> csrf.disable()) // Stateless bearer authentication, no session cookies.
        .cors(cors -> cors.configurationSource(corsSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
        .exceptionHandling(
            errors ->
                errors.authenticationEntryPoint(
                    (request, response, exception) -> {
                      response.setStatus(401);
                      response.setContentType("application/json");
                      mapper.writeValue(
                          response.getOutputStream(),
                          Map.of("error", "valid demo bearer token required"));
                    }))
        .addFilterBefore(
            new DemoTokenFilter(properties.demoToken()), UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private UrlBasedCorsConfigurationSource corsSource() {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(
        List.of(
            "http://localhost:13000",
            "http://127.0.0.1:13000",
            "http://localhost:18081",
            "http://127.0.0.1:18081"));
    cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cors.setExposedHeaders(List.of("X-Request-ID", "X-Virtual-Thread"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
