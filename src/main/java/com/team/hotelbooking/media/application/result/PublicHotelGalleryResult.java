package com.team.hotelbooking.media.application.result;

import java.util.List;
import java.util.UUID;

public record PublicHotelGalleryResult(UUID hotelId, List<Image> images, List<Album> albums) {
  public record Image(UUID mediaId, UUID albumId, int sortOrder, boolean cover) {}

  public record Album(UUID id, String name, String description, int sortOrder) {}
}
