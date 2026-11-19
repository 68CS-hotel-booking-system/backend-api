package com.team.hotelbooking.identity.application.usecase;

public interface LogoutUseCase {

  void execute(String refreshToken);
}
