package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.CancelBookingResult;

public record CancelBookingResponse(String status, String currency, Cancellation cancellation) {
  public record Cancellation(long cancellationFeeAmount, long refundAmount, String refundStatus) {}

  public static CancelBookingResponse from(CancelBookingResult result) {
    return new CancelBookingResponse(
        result.status(),
        result.currency(),
        new Cancellation(0, result.refundAmount(), result.refundStatus()));
  }
}
