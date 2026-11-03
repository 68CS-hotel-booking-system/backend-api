package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.result.CurrentUserResult;
import java.util.UUID;

public interface GetCurrentUserUseCase {
  CurrentUserResult execute(UUID userId);
}
