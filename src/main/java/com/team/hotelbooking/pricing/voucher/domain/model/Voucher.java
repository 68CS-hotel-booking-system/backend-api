package com.team.hotelbooking.pricing.voucher.domain.model;

import com.team.hotelbooking.pricing.voucher.domain.exception.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

public record Voucher(
    UUID id,
    UUID hotelId,
    String code,
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    Instant startsAt,
    Instant endsAt,
    int usageLimit,
    boolean enabled,
    UUID createdBy,
    Instant createdAt,
    long version) {
  public static String normalize(String code) {
    if (code == null || code.isBlank()) return null;
    String value = code.trim().toUpperCase(Locale.ROOT);
    if (!value.matches("[A-Z0-9][A-Z0-9_-]{2,29}"))
      throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    return value;
  }

  public static Voucher create(
      UUID hotel,
      UUID actor,
      String code,
      String name,
      String type,
      long value,
      Long cap,
      long minimum,
      Instant start,
      Instant end,
      int limit,
      Instant now) {
    String normalized = normalize(code);
    if (hotel == null
        || actor == null
        || normalized == null
        || name == null
        || name.isBlank()
        || name.trim().length() > 100
        || !Set.of("PERCENT", "FIXED").contains(type == null ? "" : type)
        || value < 1
        || value > 1_000_000_000_000L
        || minimum < 0
        || minimum > 1_000_000_000_000L
        || start == null
        || end == null
        || !end.isAfter(start)
        || !end.isAfter(now)
        || limit < 1
        || limit > 1_000_000
        || ("PERCENT".equals(type) && value > 99)
        || (cap != null && (cap < 1 || cap > 1_000_000_000_000L))
        || ("FIXED".equals(type) && cap != null))
      throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    return new Voucher(
        UUID.randomUUID(),
        hotel,
        normalized,
        name.trim(),
        type,
        value,
        cap,
        minimum,
        start,
        end,
        limit,
        true,
        actor,
        now,
        0);
  }

  public long discount(long roomAmount, Instant now, long occupied) {
    if (!enabled || now.isBefore(startsAt) || !now.isBefore(endsAt) || occupied >= usageLimit)
      throw new VoucherException(VoucherErrorCode.VOUCHER_UNAVAILABLE);
    if (roomAmount < minOrderAmount || roomAmount <= 1)
      throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    long amount =
        "PERCENT".equals(discountType)
            ? BigInteger.valueOf(roomAmount)
                .multiply(BigInteger.valueOf(discountValue))
                .divide(BigInteger.valueOf(100))
                .longValueExact()
            : discountValue;
    if (maxDiscountAmount != null) amount = Math.min(amount, maxDiscountAmount);
    amount = Math.min(amount, roomAmount - 1);
    if (amount < 1) throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    return amount;
  }
}
