package com.team.hotelbooking.media.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RoomTypeImage(
    UUID id, UUID hotelId, UUID roomTypeId, UUID mediaId, Instant createdAt) {}
