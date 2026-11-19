// CreateHotelImageAlbumCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record CreateHotelImageAlbumCommand(
    UUID actorId, UUID hotelId, String name, String description, long expectedVersion) {}
