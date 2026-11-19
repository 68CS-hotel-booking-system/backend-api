package com.team.hotelbooking.media.application.usecase;

import java.util.Optional;
import java.util.UUID;

public interface GetUserAvatarUseCase {
  Optional<UUID> execute(UUID actorId);
}
