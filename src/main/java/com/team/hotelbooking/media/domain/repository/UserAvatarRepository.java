package com.team.hotelbooking.media.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface UserAvatarRepository {
  Optional<UUID> findMediaId(UUID userId);

  void setAvatar(UUID userId, UUID mediaId);
}
