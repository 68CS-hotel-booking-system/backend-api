package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.result.VerifyEmailResult;

public interface VerifyEmailUseCase {
  VerifyEmailResult execute(String rawToken);
}
