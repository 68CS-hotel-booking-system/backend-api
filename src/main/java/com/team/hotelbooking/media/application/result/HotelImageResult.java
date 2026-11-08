// HotelImageResult.java
package com.team.hotelbooking.media.application.result;

import com.team.hotelbooking.media.domain.model.HotelImage;
import java.util.UUID;

public record HotelImageResult(UUID mediaId, int sortOrder, boolean cover) {

  public static HotelImageResult from(HotelImage image) {
    return new HotelImageResult(image.mediaId(), image.sortOrder(), image.cover());
  }
}
