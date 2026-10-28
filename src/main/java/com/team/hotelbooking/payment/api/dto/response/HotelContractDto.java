package com.team.hotelbooking.payment.api.dto.response;

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
    Instant updatedAt) {}
