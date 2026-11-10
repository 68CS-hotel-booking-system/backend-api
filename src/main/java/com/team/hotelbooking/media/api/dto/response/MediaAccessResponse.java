// MediaAccessResponse.java
package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.MediaAccessResult;
import java.time.Instant;
import java.util.UUID;

public record MediaAccessResponse(UUID mediaId, String url, Instant expiresAt) {

  public static MediaAccessResponse from(MediaAccessResult result) {

    return new MediaAccessResponse(result.mediaId(), result.url(), result.expiresAt());
  }
}
