package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HotelContractDto(
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

  public static HotelContractDto from(HotelContractResult result) {
    return new HotelContractDto(
        result.id(),
        result.contractCode(),
        result.hotelId(),
        result.ownerUserId(),
        result.commissionRate(),
        result.commissionBasis(),
        result.startsAt(),
        result.endsAt(),
        result.status(),
        result.createdByUserId(),
        result.createdAt(),
        result.updatedAt());
  }
}
