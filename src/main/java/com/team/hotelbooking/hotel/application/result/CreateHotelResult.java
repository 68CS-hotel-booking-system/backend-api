package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record CreateHotelResult(
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

  public static CreateHotelResult from(Hotel hotel) {
    return new CreateHotelResult(
        hotel.id(),
        hotel.ownerId(),
        hotel.cityId(),
        hotel.name(),
        hotel.slug(),
        hotel.description(),
        hotel.address(),
        hotel.latitude(),
        hotel.longitude(),
        hotel.starRating(),
        hotel.checkInTime(),
        hotel.checkOutTime(),
        hotel.status().name(),
        hotel.createdAt(),
        hotel.updatedAt(),
        hotel.version());
  }
}
