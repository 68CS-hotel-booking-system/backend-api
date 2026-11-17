package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.PublicHotelGalleryResult;
import java.util.List;
import java.util.UUID;

public record PublicHotelGalleryResponse(UUID hotelId, List<Image> images, List<Album> albums) {
  public record Image(UUID mediaId, UUID albumId, int sortOrder, boolean cover) {}

  public record Album(UUID id, String name, String description, int sortOrder) {}

  public static PublicHotelGalleryResponse from(PublicHotelGalleryResult result) {
    return new PublicHotelGalleryResponse(
        result.hotelId(),
        result.images().stream()
            .map(i -> new Image(i.mediaId(), i.albumId(), i.sortOrder(), i.cover()))
            .toList(),
        result.albums().stream()
            .map(a -> new Album(a.id(), a.name(), a.description(), a.sortOrder()))
            .toList());
  }
}
