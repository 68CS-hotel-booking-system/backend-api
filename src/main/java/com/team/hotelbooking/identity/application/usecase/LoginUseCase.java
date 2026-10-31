package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.LoginCommand;
import com.team.hotelbooking.identity.application.result.LoginResult;

public interface LoginUseCase {

  LoginResult execute(LoginCommand command);
}
