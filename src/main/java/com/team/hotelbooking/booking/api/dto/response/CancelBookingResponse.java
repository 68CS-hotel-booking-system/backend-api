package com.team.hotelbooking.booking.api.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;

public record CancelBookingResponse(
    @JsonUnwrapped BookingDetailResponse booking, CancellationResponse cancellation) {

  public record CancellationResponse(
      long cancellationFeeAmount, long refundAmount, String refundStatus) {}

  public static CancelBookingResponse from(CancelBookingResult result) {
    return new CancelBookingResponse(
        BookingDetailResponse.from(result.booking()),
        new CancellationResponse(
            result.cancellationFeeAmount(), result.refundAmount(), result.refundStatus()));
  }
}
