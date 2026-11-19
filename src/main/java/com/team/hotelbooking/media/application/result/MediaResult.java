// MediaResult.java
package com.team.hotelbooking.media.application.result;

import com.team.hotelbooking.media.domain.model.MediaObject;
import java.time.Instant;
import java.util.UUID;

public record MediaResult(
    UUID id, String purpose, String status, String contentType, long sizeBytes, Instant createdAt) {

  public static MediaResult from(MediaObject media) {
    return new MediaResult(
        media.id(),
        media.purpose().name(),
        media.status().name(),
        media.contentType(),
        media.sizeBytes(),
        media.createdAt());
  }
}
