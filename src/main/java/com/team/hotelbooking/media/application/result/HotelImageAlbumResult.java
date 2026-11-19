// HotelImageAlbumResult.java
package com.team.hotelbooking.media.application.result;

import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import java.time.Instant;
import java.util.UUID;

public record HotelImageAlbumResult(
    UUID id,
    UUID hotelId,
    String name,
    String description,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt) {

  public static HotelImageAlbumResult from(HotelImageAlbum album) {

    return new HotelImageAlbumResult(
        album.id(),
        album.hotelId(),
        album.name(),
        album.description(),
        album.sortOrder(),
        album.createdAt(),
        album.updatedAt());
  }
}
