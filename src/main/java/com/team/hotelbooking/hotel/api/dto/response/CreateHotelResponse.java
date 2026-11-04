package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.CreateHotelResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record CreateHotelResponse(
    UUID id,
    UUID ownerId,
    UUID cityId,
    String name,
    String slug,
    String description,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static CreateHotelResponse from(CreateHotelResult result) {
    return new CreateHotelResponse(
        result.id(),
        result.ownerId(),
        result.cityId(),
        result.name(),
        result.slug(),
        result.description(),
        result.address(),
        result.latitude(),
        result.longitude(),
        result.starRating(),
        result.checkInTime(),
        result.checkOutTime(),
        result.status(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }
}
