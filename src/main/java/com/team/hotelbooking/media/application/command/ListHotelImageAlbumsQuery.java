// ListHotelImageAlbumsQuery.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record ListHotelImageAlbumsQuery(UUID actorId, UUID hotelId) {}
