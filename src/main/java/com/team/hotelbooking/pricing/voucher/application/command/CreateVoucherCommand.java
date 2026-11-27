package com.team.hotelbooking.pricing.voucher.application.command;

import java.time.Instant;
import java.util.UUID;

public record CreateVoucherCommand(
    UUID actorId,
    UUID hotelId,
    String code,
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    Instant startsAt,
    Instant endsAt,
    int usageLimit) {}
