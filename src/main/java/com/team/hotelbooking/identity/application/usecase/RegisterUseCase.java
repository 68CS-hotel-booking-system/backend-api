package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.RegisterCommand;
import com.team.hotelbooking.identity.application.result.RegisterResult;

public interface RegisterUseCase {
  RegisterResult execute(RegisterCommand command);
}
