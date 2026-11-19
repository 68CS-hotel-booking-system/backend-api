// UploadHotelImageCommand.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record UploadHotelImageCommand(
    UUID actorId, UUID hotelId, String originalFilename, byte[] content) {}
