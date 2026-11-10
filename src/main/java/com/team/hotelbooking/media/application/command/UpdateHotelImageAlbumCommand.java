// UpdateHotelImageAlbumCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record UpdateHotelImageAlbumCommand(
    UUID actorId,
    UUID hotelId,
    UUID albumId,
    String name,
    String description,
    long expectedVersion) {}
