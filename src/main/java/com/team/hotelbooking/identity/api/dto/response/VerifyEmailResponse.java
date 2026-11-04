package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.VerifyEmailResult;

public record VerifyEmailResponse(boolean accepted) {

  public static VerifyEmailResponse from(VerifyEmailResult result) {
    return new VerifyEmailResponse(result.accepted());
  }
}
