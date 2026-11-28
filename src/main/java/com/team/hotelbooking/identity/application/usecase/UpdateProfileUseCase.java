package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.UpdateProfileCommand;
import com.team.hotelbooking.identity.application.result.CurrentUserResult;

public interface UpdateProfileUseCase {
  CurrentUserResult execute(UpdateProfileCommand command);
}
