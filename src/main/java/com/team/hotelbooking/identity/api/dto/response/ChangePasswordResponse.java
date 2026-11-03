package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.ChangePasswordResult;

public record ChangePasswordResponse(boolean changed) {

  public static ChangePasswordResponse from(ChangePasswordResult result) {
    return new ChangePasswordResponse(result.changed());
  }
}
