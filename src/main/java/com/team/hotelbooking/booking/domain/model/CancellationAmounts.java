package com.team.hotelbooking.booking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public record CancellationAmounts(long fee, long refund, long commission) {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Duration FREE_CANCELLATION_NOTICE = Duration.ofHours(24);

  public static CancellationAmounts calculate(
      Booking booking,
      Instant now,
      boolean cancelledByHotel,
      long capturedAmount,
      BigDecimal commissionRate) {
    Instant freeUntil =
        booking
            .checkIn()
            .atTime(booking.standardCheckInTime())
            .atZone(BUSINESS_ZONE)
            .toInstant()
            .minus(FREE_CANCELLATION_NOTICE);
    long fee = cancelledByHotel || !now.isAfter(freeUntil) ? 0 : capturedAmount;
    long commission =
        BigDecimal.valueOf(fee)
            .multiply(commissionRate)
            .movePointLeft(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    return new CancellationAmounts(fee, capturedAmount - fee, commission);
  }
}
