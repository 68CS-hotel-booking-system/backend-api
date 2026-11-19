package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.AuthCookieManager;
import com.team.hotelbooking.identity.api.dto.response.LoginResponse;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.usecase.impl.GoogleAccountService;
import com.team.hotelbooking.identity.infrastructure.integration.GoogleIdTokens;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {
  private final GoogleIdTokens google;
  private final GoogleAccountService accounts;
  private final AuthCookieManager authCookies;
  private final OpaqueTokens tokens;

  @GetMapping
  public ResponseEntity<ApiResponse<GoogleChallenge>> challenge(
      @CookieValue(name = AuthCookieManager.GOOGLE_NONCE_COOKIE_NAME, required = false)
          String currentNonce,
      HttpServletRequest request,
      HttpServletResponse response) {
    String clientId = google.clientId();
    String nonce = "";
    if (!clientId.isBlank()) {
      nonce =
          currentNonce == null || currentNonce.isBlank() ? tokens.generate().raw() : currentNonce;
      authCookies.setGoogleNonce(response, request, nonce, Duration.ofMinutes(10));
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            ApiResponse.success(
                "Cấu hình đăng nhập Google.", new GoogleChallenge(clientId, nonce)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<LoginResponse>> signIn(
      @Valid @RequestBody GoogleSignInRequest body,
      @CookieValue(name = AuthCookieManager.GOOGLE_NONCE_COOKIE_NAME, required = false)
          String nonce,
      HttpServletRequest request,
      HttpServletResponse response) {
    var account = google.verify(body.credential(), nonce);
    var login = accounts.signIn(account, body.phone());
    authCookies.setGoogleNonce(response, request, "", Duration.ZERO);
    authCookies.addAuthCookies(
        response, request, login.refreshToken(), login.rememberMe(), login.refreshExpiresAt());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            ApiResponse.success(
                "Đăng nhập bằng Google thành công.", LoginResponse.from(login, false)));
  }

  public record GoogleChallenge(String clientId, String nonce) {}

  public record GoogleSignInRequest(
      @NotBlank @Size(max = 8192) String credential, @Size(max = 20) String phone) {

    @Override
    public String toString() {
      return "GoogleSignInRequest[credential=REDACTED]";
    }
  }
}
