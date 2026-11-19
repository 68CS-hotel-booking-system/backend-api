package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.CustomAmenity;
import java.time.Instant;
import java.util.UUID;

public record CustomAmenityResult(
    UUID id,
    UUID hotelId,
    UUID roomId,
    String name,
    String description,
    boolean attached,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static CustomAmenityResult from(CustomAmenity item) {
    return new CustomAmenityResult(
        item.id(),
        item.hotelId(),
        item.roomId(),
        item.name(),
        item.description(),
        item.attached(),
        item.createdAt(),
        item.updatedAt(),
        item.version());
  }
}
