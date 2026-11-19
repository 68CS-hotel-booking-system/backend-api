package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.CustomAmenityResult;
import java.time.Instant;
import java.util.UUID;

public record CustomAmenityResponse(
    UUID id,
    UUID hotelId,
    UUID roomId,
    String name,
    String description,
    boolean attached,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static CustomAmenityResponse from(CustomAmenityResult result) {
    return new CustomAmenityResponse(
        result.id(),
        result.hotelId(),
        result.roomId(),
        result.name(),
        result.description(),
        result.attached(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }
}
