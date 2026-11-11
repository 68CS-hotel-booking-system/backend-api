package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ManagementAmenitiesResult;
import java.util.List;
import java.util.UUID;

public record ManagementAmenitiesResponse(
    UUID hotelId,
    UUID roomId,
    List<ListAmenitiesResponse> catalogAmenities,
    List<CustomAmenityResponse> customAmenities,
    List<ListAmenitiesResponse> inheritedAmenities) {
  public static ManagementAmenitiesResponse from(ManagementAmenitiesResult result) {
    return new ManagementAmenitiesResponse(
        result.hotelId(),
        result.roomId(),
        ListAmenitiesResponse.from(result.catalogAmenities()),
        result.customAmenities().stream().map(CustomAmenityResponse::from).toList(),
        ListAmenitiesResponse.from(result.inheritedAmenities()));
  }
}
