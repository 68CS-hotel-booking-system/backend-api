// HotelImageAlbumResponse.java
package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;
import java.time.Instant;
import java.util.UUID;

public record HotelImageAlbumResponse(
    UUID id,
    UUID hotelId,
    String name,
    String description,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt) {

  public static HotelImageAlbumResponse from(HotelImageAlbumResult result) {

    return new HotelImageAlbumResponse(
        result.id(),
        result.hotelId(),
        result.name(),
        result.description(),
        result.sortOrder(),
        result.createdAt(),
        result.updatedAt());
  }
}
