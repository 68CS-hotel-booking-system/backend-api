package com.team.hotelbooking.identity.api;

import com.team.hotelbooking.identity.domain.exception.IdentityException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieManager {

  public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
  public static final String AUTH_COOKIE_PATH = "/api/v1/auth";
  public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  public static final String REQUESTED_WITH_HEADER = "X-Requested-With";

  private final Clock clock;
  private final boolean configuredSecure;

  public AuthCookieManager(
      Clock clock, @Value("${auth.cookie.secure:false}") boolean configuredSecure) {
    this.clock = clock;
    this.configuredSecure = configuredSecure;
  }

  public void addAuthCookies(
      HttpServletResponse response,
      HttpServletRequest request,
      String rawRefreshToken,
      boolean rememberMe,
      Instant refreshExpiresAt) {

    boolean secure = isSecure(request);

    ResponseCookie.ResponseCookieBuilder refreshCookieBuilder =
        ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, rawRefreshToken)
            .httpOnly(true)
            .secure(secure)
            .path(AUTH_COOKIE_PATH)
            .sameSite("Lax");

    String csrfToken = UUID.randomUUID().toString();
    ResponseCookie.ResponseCookieBuilder csrfCookieBuilder =
        ResponseCookie.from(CSRF_COOKIE_NAME, csrfToken)
            .httpOnly(false)
            .secure(secure)
            .path("/")
            .sameSite("Lax");

    if (rememberMe && refreshExpiresAt != null) {
      long maxAge = Math.max(0, Duration.between(clock.instant(), refreshExpiresAt).getSeconds());
      refreshCookieBuilder.maxAge(maxAge);
      csrfCookieBuilder.maxAge(maxAge);
    } else {
      refreshCookieBuilder.maxAge(-1);
      csrfCookieBuilder.maxAge(-1);
    }

    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieBuilder.build().toString());
    response.addHeader(HttpHeaders.SET_COOKIE, csrfCookieBuilder.build().toString());
  }

  public void clearAuthCookies(HttpServletResponse response, HttpServletRequest request) {
    boolean secure = isSecure(request);

    ResponseCookie clearRefresh =
        ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .path(AUTH_COOKIE_PATH)
            .maxAge(0)
            .sameSite("Lax")
            .build();

    ResponseCookie clearCsrf =
        ResponseCookie.from(CSRF_COOKIE_NAME, "")
            .httpOnly(false)
            .secure(secure)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, clearCsrf.toString());
  }

  public Optional<String> extractRefreshToken(HttpServletRequest request) {
    if (request == null || request.getCookies() == null) {
      return Optional.empty();
    }

    return Arrays.stream(request.getCookies())
        .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(val -> val != null && !val.isBlank())
        .findFirst();
  }

  public void validateCsrf(HttpServletRequest request) {
    if (request == null) {
      return;
    }

    String csrfCookieValue = null;
    if (request.getCookies() != null) {
      csrfCookieValue =
          Arrays.stream(request.getCookies())
              .filter(cookie -> CSRF_COOKIE_NAME.equals(cookie.getName()))
              .map(Cookie::getValue)
              .filter(val -> val != null && !val.isBlank())
              .findFirst()
              .orElse(null);
    }

    String csrfHeaderValue = request.getHeader(CSRF_HEADER_NAME);
    String requestedWith = request.getHeader(REQUESTED_WITH_HEADER);

    // If XMLHttpRequest header is present, it cannot be forged by standard cross-site HTML forms
    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
      return;
    }

    if (csrfCookieValue == null
        || csrfHeaderValue == null
        || !csrfCookieValue.equals(csrfHeaderValue)) {
      throw IdentityException.invalid("Yêu cầu không vượt qua kiểm tra bảo vệ CSRF.");
    }
  }

  private boolean isSecure(HttpServletRequest request) {
    if (configuredSecure) {
      return true;
    }
    if (request == null) {
      return false;
    }
    return request.isSecure()
        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))
        || "https".equalsIgnoreCase(request.getScheme());
  }
}
