package com.team.hotelbooking.booking.application.result;

public record CancelBookingResult(
    String status, String currency, long refundAmount, String refundStatus) {
  public CancelBookingResult(String status, String currency) {
    this(status, currency, 0, "NOT_REQUIRED");
  }
}
