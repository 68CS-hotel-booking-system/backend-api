package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ManagementHotelResult(
    UUID id,
    UUID ownerId,
    UUID cityId,
    String name,
    String slug,
    String description,
    String address,
    String contactPhone,
    String contactEmail,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version,
    String accessLevel) {

  public ManagementHotelResult(
      UUID id,
      UUID ownerId,
      UUID cityId,
      String name,
      String slug,
      String description,
      String address,
      String contactPhone,
      String contactEmail,
      BigDecimal latitude,
      BigDecimal longitude,
      Short starRating,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      String status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this(
        id,
        ownerId,
        cityId,
        name,
        slug,
        description,
        address,
        contactPhone,
        contactEmail,
        latitude,
        longitude,
        starRating,
        checkInTime,
        checkOutTime,
        status,
        createdAt,
        updatedAt,
        version,
        null);
  }

  public static ManagementHotelResult from(Hotel hotel) {
    return from(hotel, null);
  }

  public static ManagementHotelResult from(Hotel hotel, String accessLevel) {
    return new ManagementHotelResult(
        hotel.id(),
        hotel.ownerId(),
        hotel.cityId(),
        hotel.name(),
        hotel.slug(),
        hotel.description(),
        hotel.address(),
        hotel.contactPhone(),
        hotel.contactEmail(),
        hotel.latitude(),
        hotel.longitude(),
        hotel.starRating(),
        hotel.checkInTime(),
        hotel.checkOutTime(),
        hotel.status().name(),
        hotel.createdAt(),
        hotel.updatedAt(),
        hotel.version(),
        accessLevel);
  }
}
