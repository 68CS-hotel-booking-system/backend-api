package com.team.hotelbooking.booking.application.result;

public record CancelBookingResult(
    BookingDetailResult booking,
    long cancellationFeeAmount,
    long refundAmount,
    String refundStatus) {

  public String status() {
    return booking.status();
  }
}
