package com.team.hotelbooking.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HotelContract(
    UUID id,
    String contractCode,
    UUID hotelId,
    UUID ownerUserId,
    BigDecimal commissionRate,
    CommissionBasis commissionBasis,
    Instant startsAt,
    Instant endsAt,
    ContractStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {

  public enum CommissionBasis {
    BEFORE_DISCOUNT,
    AFTER_DISCOUNT,
    AFTER_HOTEL_DISCOUNT
  }

  public enum ContractStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    TERMINATED
  }
}
