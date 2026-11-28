package com.team.hotelbooking.booking.application.command;

import java.util.UUID;

public record CancelBookingCommand(
    UUID customerId,
    UUID bookingId,
    Long expectedVersion,
    Long expectedRefundAmount,
    String reason) {
  public CancelBookingCommand(UUID customerId, UUID bookingId) {
    this(customerId, bookingId, null, null, null);
  }
}
