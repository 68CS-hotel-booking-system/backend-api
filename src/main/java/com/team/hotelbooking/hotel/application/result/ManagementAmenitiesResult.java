package com.team.hotelbooking.hotel.application.result;

import java.util.List;
import java.util.UUID;

public record ManagementAmenitiesResult(
    UUID hotelId,
    UUID roomId,
    List<ListAmenitiesResult> catalogAmenities,
    List<CustomAmenityResult> customAmenities,
    List<ListAmenitiesResult> inheritedAmenities) {}
