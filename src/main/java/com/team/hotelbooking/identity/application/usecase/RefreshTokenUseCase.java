package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.result.LoginResult;

public interface RefreshTokenUseCase {

  LoginResult execute(String rawRefreshToken);
}
