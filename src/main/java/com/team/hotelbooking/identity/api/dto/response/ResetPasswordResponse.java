package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.ResetPasswordResult;

public record ResetPasswordResponse(boolean reset) {

  public static ResetPasswordResponse from(ResetPasswordResult result) {
    return new ResetPasswordResponse(result.reset());
  }
}
