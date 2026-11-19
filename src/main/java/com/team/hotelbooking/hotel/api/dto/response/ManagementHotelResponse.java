package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ManagementHotelResponse(
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

  public static ManagementHotelResponse from(ManagementHotelResult result) {
    return new ManagementHotelResponse(
        result.id(),
        result.ownerId(),
        result.cityId(),
        result.name(),
        result.slug(),
        result.description(),
        result.address(),
        result.contactPhone(),
        result.contactEmail(),
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

  public static PageResponse<ManagementHotelResponse> from(
      PageResult<ManagementHotelResult> result) {
    return PageResponse.from(result.map(ManagementHotelResponse::from));
  }
}
