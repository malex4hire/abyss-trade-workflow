package com.abyssapplied.trade.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "trade.mode", havingValue = "api", matchIfMissing = true)
public class RequestTraceFilter extends OncePerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(RequestTraceFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    response.setHeader("X-Request-ID", requestId);
    response.setHeader("X-Virtual-Thread", Boolean.toString(Thread.currentThread().isVirtual()));
    LOG.info(
        "requestId={} method={} path={}", requestId, request.getMethod(), request.getRequestURI());
    chain.doFilter(request, response);
  }
}
