package com.team.hotelbooking.pricing.voucher.application.result;

import com.team.hotelbooking.pricing.voucher.domain.model.Voucher;
import java.time.Instant;
import java.util.UUID;

public record VoucherResult(
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
    long occupiedUses,
    boolean enabled,
    String status,
    long version) {
  public static VoucherResult from(Voucher v, long count, Instant now) {
    return new VoucherResult(
        v.id(),
        v.hotelId(),
        v.code(),
        v.name(),
        v.discountType(),
        v.discountValue(),
        v.maxDiscountAmount(),
        v.minOrderAmount(),
        v.startsAt(),
        v.endsAt(),
        v.usageLimit(),
        count,
        v.enabled(),
        !v.enabled()
            ? "PAUSED"
            : !now.isBefore(v.endsAt())
                ? "EXPIRED"
                : now.isBefore(v.startsAt())
                    ? "SCHEDULED"
                    : count >= v.usageLimit() ? "EXHAUSTED" : "ACTIVE",
        v.version());
  }
}
