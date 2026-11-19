// ReplaceHotelImagesCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.List;
import java.util.UUID;

public record ReplaceHotelImagesCommand(
    UUID actorId, UUID hotelId, List<Image> images, long expectedVersion) {

  public ReplaceHotelImagesCommand {
    images = List.copyOf(images);
  }

  public record Image(UUID mediaId, UUID albumId, int sortOrder, boolean cover) {}
}
