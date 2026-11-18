package com.team.hotelbooking.media.application.result;

import com.team.hotelbooking.media.domain.model.RoomTypeImage;
import java.time.Instant;
import java.util.UUID;

public record RoomTypeImageResult(UUID mediaId, Instant createdAt) {
  public static RoomTypeImageResult from(RoomTypeImage image) {
    return new RoomTypeImageResult(image.mediaId(), image.createdAt());
  }
}
