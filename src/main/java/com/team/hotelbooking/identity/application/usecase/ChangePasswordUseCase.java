package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.ChangePasswordCommand;
import com.team.hotelbooking.identity.application.result.ChangePasswordResult;

public interface ChangePasswordUseCase {

  ChangePasswordResult execute(ChangePasswordCommand command);
}
