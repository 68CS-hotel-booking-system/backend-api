package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
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
    LocalTime checkOutTime) {

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
        result.checkOutTime());
  }

  public static PageResponse<GetPublicHotelResponse> from(PageResult<GetPublicHotelResult> result) {
    return new PageResponse<>(
        result.content().stream().map(GetPublicHotelResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.first(),
        result.last());
  }
}
