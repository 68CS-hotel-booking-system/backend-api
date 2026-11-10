// ReorderHotelImageAlbumsCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.List;
import java.util.UUID;

public record ReorderHotelImageAlbumsCommand(
    UUID actorId, UUID hotelId, List<UUID> albumIds, long expectedVersion) {

  public ReorderHotelImageAlbumsCommand {
    albumIds = List.copyOf(albumIds);
  }
}
