package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record RoomTypeImagesQuery(UUID hotelId, UUID roomTypeId, UUID actorId) {}
