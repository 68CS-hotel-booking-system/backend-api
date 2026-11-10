package com.team.hotelbooking.media.domain.repository;

import com.team.hotelbooking.media.domain.model.MediaObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository {

  void add(MediaObject media);

  Optional<MediaObject> findById(UUID mediaId);

  void markReady(UUID mediaId);

  void markDeleted(UUID mediaId, Instant deletedAt);
}
