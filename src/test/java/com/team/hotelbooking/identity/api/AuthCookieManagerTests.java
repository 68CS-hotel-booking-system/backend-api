package com.team.hotelbooking.identity.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.hotelbooking.identity.domain.exception.IdentityException;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Kiểm thử AuthCookieManager cho refresh token và CSRF")
class AuthCookieManagerTests {

  private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String RAW_REFRESH = "sample-raw-refresh-token";

  private AuthCookieManager manager;

  @BeforeEach
  void setUp() {
    manager = new AuthCookieManager(FIXED_CLOCK, false);
  }

  @Test
  void shouldCreatePersistentCookieWhenRememberMeIsTrue() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Instant expiresAt = NOW.plus(Duration.ofDays(7));

    // When
    manager.addAuthCookies(response, request, RAW_REFRESH, true, expiresAt);

    // Then
    List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertEquals(2, setCookies.size());

    String refreshCookie =
        setCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();

    assertTrue(refreshCookie.contains("refresh_token=" + RAW_REFRESH));
    assertTrue(refreshCookie.contains("Path=/api/v1/auth"));
    assertTrue(refreshCookie.contains("HttpOnly"));
    assertTrue(refreshCookie.contains("SameSite=Lax"));
    assertTrue(refreshCookie.contains("Max-Age=" + Duration.ofDays(7).getSeconds()));

    String csrfCookie =
        setCookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
    assertTrue(csrfCookie.contains("Path=/"));
    assertFalse(csrfCookie.contains("HttpOnly"));
  }

  @Test
  void shouldCreateSessionCookieWhenRememberMeIsFalse() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Instant expiresAt = NOW.plus(Duration.ofDays(7));

    // When
    manager.addAuthCookies(response, request, RAW_REFRESH, false, expiresAt);

    // Then
    List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    String refreshCookie =
        setCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();

    assertTrue(refreshCookie.contains("refresh_token=" + RAW_REFRESH));
    assertTrue(refreshCookie.contains("Path=/api/v1/auth"));
    assertTrue(refreshCookie.contains("HttpOnly"));
    // Session cookie: no Max-Age or Expires
    assertFalse(refreshCookie.contains("Max-Age"));
    assertFalse(refreshCookie.contains("Expires"));
  }

  @Test
  void shouldClearCookiesWithMaxAgeZero() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    manager.clearAuthCookies(response, request);

    // Then
    List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertEquals(2, setCookies.size());

    String clearRefresh =
        setCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();
    assertTrue(clearRefresh.contains("Path=/api/v1/auth"));
    assertTrue(clearRefresh.contains("Max-Age=0"));

    String clearCsrf =
        setCookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
    assertTrue(clearCsrf.contains("Path=/"));
    assertTrue(clearCsrf.contains("Max-Age=0"));
  }

  @Test
  void shouldExtractRefreshTokenFromCookies() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(
        new Cookie("other", "value"), new Cookie("refresh_token", "extracted-token"));

    // When
    Optional<String> token = manager.extractRefreshToken(request);

    // Then
    assertTrue(token.isPresent());
    assertEquals("extracted-token", token.get());
  }

  @Test
  void shouldPassCsrfValidationWithRequestedWithHeader() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Requested-With", "XMLHttpRequest");

    // When & Then
    manager.validateCsrf(request); // Should not throw
  }

  @Test
  void shouldPassCsrfValidationWithMatchingXsrfToken() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("XSRF-TOKEN", "csrf-secret-123"));
    request.addHeader("X-XSRF-TOKEN", "csrf-secret-123");

    // When & Then
    manager.validateCsrf(request); // Should not throw
  }

  @Test
  void shouldThrowWhenCsrfTokenMismatches() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("XSRF-TOKEN", "csrf-secret-123"));
    request.addHeader("X-XSRF-TOKEN", "wrong-secret");

    // When & Then
    assertThrows(IdentityException.class, () -> manager.validateCsrf(request));
  }

  @Test
  void shouldSetSecureWhenRequestHasHttpsHeader() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-Proto", "https");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    manager.addAuthCookies(response, request, RAW_REFRESH, false, null);

    // Then
    String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertNotNull(cookie);
    assertTrue(cookie.contains("Secure"));
  }
}
