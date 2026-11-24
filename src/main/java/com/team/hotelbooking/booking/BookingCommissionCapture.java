package com.team.hotelbooking.booking;

import java.time.Instant;
import java.util.UUID;

public interface BookingCommissionCapture {
  void capture(
      UUID bookingId,
      UUID hotelId,
      long roomAmount,
      long discountAmount,
      long totalAmount,
      Instant at);

  class ContractUnavailableException extends RuntimeException {}
}
