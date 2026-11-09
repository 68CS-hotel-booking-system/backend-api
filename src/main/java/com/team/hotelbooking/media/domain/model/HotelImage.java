package com.team.hotelbooking.media.domain.model;

import java.util.UUID;

public record HotelImage(UUID mediaId, int sortOrder, boolean cover) {}
