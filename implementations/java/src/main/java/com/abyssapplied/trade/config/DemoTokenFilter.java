package com.abyssapplied.trade.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Models ONE local identity only. Production requires a real identity provider. */
final class DemoTokenFilter extends OncePerRequestFilter {
  private final byte[] expected;

  DemoTokenFilter(String token) {
    expected = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader("Authorization");
    if (supplied != null
        && MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
      var authentication =
          new UsernamePasswordAuthenticationToken(
              "demo-importer", null, List.of(new SimpleGrantedAuthority("ROLE_DEMO")));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    chain.doFilter(request, response);
  }
}
