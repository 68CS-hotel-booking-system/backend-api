package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.VerifyEmailResult;
import java.time.Instant;

public record VerifyEmailResponse(
    boolean accepted,
    String accessToken,
    String tokenType,
    Instant expiresAt,
    LoginResponse.UserResponse user) {

  public static VerifyEmailResponse from(VerifyEmailResult result) {
    if (result.session() == null) {
      return new VerifyEmailResponse(result.accepted(), null, null, null, null);
    }

    LoginResponse loginResp = LoginResponse.from(result.session(), false);
    return new VerifyEmailResponse(
        result.accepted(),
        loginResp.accessToken(),
        loginResp.tokenType(),
        loginResp.expiresAt(),
        loginResp.user());
  }
}
