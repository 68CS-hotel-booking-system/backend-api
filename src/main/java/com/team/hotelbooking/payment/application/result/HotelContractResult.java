package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HotelContractResult(
    UUID id,
    String contractCode,
    UUID hotelId,
    UUID ownerUserId,
    BigDecimal commissionRate,
    HotelContract.CommissionBasis commissionBasis,
    Instant startsAt,
    Instant endsAt,
    HotelContract.ContractStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {

  public static HotelContractResult from(HotelContract domain) {
    return new HotelContractResult(
        domain.id(),
        domain.contractCode(),
        domain.hotelId(),
        domain.ownerUserId(),
        domain.commissionRate(),
        domain.commissionBasis(),
        domain.startsAt(),
        domain.endsAt(),
        domain.status(),
        domain.createdByUserId(),
        domain.createdAt(),
        domain.updatedAt());
  }
}
