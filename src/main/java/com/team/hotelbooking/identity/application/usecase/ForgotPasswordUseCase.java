package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.result.ForgotPasswordResult;

public interface ForgotPasswordUseCase {

  ForgotPasswordResult execute(String email);
}
