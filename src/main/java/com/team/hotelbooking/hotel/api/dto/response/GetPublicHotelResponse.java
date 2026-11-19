package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record GetPublicHotelResponse(
    UUID id,
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
    List<ListAmenitiesResponse> amenities) {

  public static GetPublicHotelResponse from(GetPublicHotelResult result) {
    return new GetPublicHotelResponse(
        result.id(),
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
        ListAmenitiesResponse.from(result.amenities()));
  }
}
