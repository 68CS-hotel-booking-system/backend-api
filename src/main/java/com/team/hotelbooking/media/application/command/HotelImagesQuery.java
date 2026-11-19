// HotelImagesQuery.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record HotelImagesQuery(UUID actorId, UUID hotelId) {}
