package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.ResendUserVerificationEmailResult;

public record ResendUserVerificationEmailResponse(boolean accepted) {

  public static ResendUserVerificationEmailResponse from(ResendUserVerificationEmailResult result) {
    return new ResendUserVerificationEmailResponse(result.accepted());
  }
}
