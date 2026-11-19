package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.ForgotPasswordResult;

public record ForgotPasswordResponse(boolean accepted) {

  public static ForgotPasswordResponse from(ForgotPasswordResult result) {
    return new ForgotPasswordResponse(result.accepted());
  }
}
