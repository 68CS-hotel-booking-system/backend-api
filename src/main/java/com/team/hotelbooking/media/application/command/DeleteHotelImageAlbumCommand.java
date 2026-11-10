// DeleteHotelImageAlbumCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record DeleteHotelImageAlbumCommand(
    UUID actorId, UUID hotelId, UUID albumId, long expectedVersion) {}
