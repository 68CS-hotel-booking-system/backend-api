package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.RoomTypeImageResult;
import java.time.Instant;
import java.util.UUID;

public record RoomTypeImageResponse(UUID mediaId, Instant createdAt) {
  public static RoomTypeImageResponse from(RoomTypeImageResult result) {
    return new RoomTypeImageResponse(result.mediaId(), result.createdAt());
  }
}
