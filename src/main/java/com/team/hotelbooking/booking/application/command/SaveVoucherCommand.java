package com.team.hotelbooking.booking.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaveVoucherCommand(
    UUID actorId,
    UUID hotelId,
    String code,
    boolean create,
    String name,
    String discountType,
    BigDecimal percentageOff,
    Long fixedAmount,
    Long maxDiscountAmount,
    Long minBookingAmount,
    Integer usageLimit,
    Integer perUserLimit,
    Instant startsAt,
    Instant endsAt,
    String status,
    String fundingSource) {}
