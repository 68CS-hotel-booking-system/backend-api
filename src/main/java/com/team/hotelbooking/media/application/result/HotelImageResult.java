// HotelImageResult.java
package com.team.hotelbooking.media.application.result;

import com.team.hotelbooking.media.domain.model.HotelImage;
import java.util.UUID;

public record HotelImageResult(UUID mediaId, UUID albumId, int sortOrder, boolean cover) {

  public static HotelImageResult from(HotelImage image) {
    return new HotelImageResult(image.mediaId(), image.albumId(), image.sortOrder(), image.cover());
  }
}
