// MediaResponse.java
package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.MediaResult;
import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
    UUID id, String purpose, String status, String contentType, long sizeBytes, Instant createdAt) {

  public static MediaResponse from(MediaResult result) {
    return new MediaResponse(
        result.id(),
        result.purpose(),
        result.status(),
        result.contentType(),
        result.sizeBytes(),
        result.createdAt());
  }
}
