package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.HotelCustomerResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.UUID;

public record HotelCustomerResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    long bookingCount,
    long hotelCount,
    long completedBookingCount,
    Instant lastBookedAt) {
  public static HotelCustomerResponse from(HotelCustomerResult result) {
    return new HotelCustomerResponse(
        result.id(),
        result.fullName(),
        result.email(),
        result.phone(),
        result.bookingCount(),
        result.hotelCount(),
        result.completedBookingCount(),
        result.lastBookedAt());
  }

  public static PageResponse<HotelCustomerResponse> from(PageResult<HotelCustomerResult> result) {
    return PageResponse.from(result.map(HotelCustomerResponse::from));
  }
}
