package com.team.hotelbooking.booking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CommissionAmounts(long basis, long platform, long owner) {
  public static CommissionAmounts calculate(
      long roomAmount, long discountAmount, long totalAmount, BigDecimal rate) {
    if (roomAmount < 0
        || discountAmount < 0
        || totalAmount < 0
        || rate == null
        || rate.signum() < 0
        || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("Invalid commission inputs");
    }
    long basis = Math.max(0, Math.subtractExact(roomAmount, discountAmount));
    long platform =
        BigDecimal.valueOf(basis)
            .multiply(rate)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValueExact();
    long owner = Math.subtractExact(totalAmount, platform);
    if (owner < 0) throw new IllegalArgumentException("Commission exceeds payment amount");
    return new CommissionAmounts(basis, platform, owner);
  }
}
