package com.team.hotelbooking.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface BookingCommissionDirectory {
  void save(
      UUID bookingId,
      UUID contractId,
      UUID ownerId,
      BigDecimal rate,
      long roomAmount,
      long discountAmount,
      long totalAmount,
      Instant at);
}
