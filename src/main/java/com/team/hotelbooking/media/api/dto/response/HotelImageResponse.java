// HotelImageResponse.java
package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.HotelImageResult;
import java.util.UUID;

public record HotelImageResponse(UUID mediaId, UUID albumId, int sortOrder, boolean cover) {

  public static HotelImageResponse from(HotelImageResult result) {

    return new HotelImageResponse(
        result.mediaId(), result.albumId(), result.sortOrder(), result.cover());
  }
}
