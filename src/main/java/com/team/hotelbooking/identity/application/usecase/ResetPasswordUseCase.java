package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.ResetPasswordCommand;
import com.team.hotelbooking.identity.application.result.ResetPasswordResult;

public interface ResetPasswordUseCase {

  ResetPasswordResult execute(ResetPasswordCommand command);
}
