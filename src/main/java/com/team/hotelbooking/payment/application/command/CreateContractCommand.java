package com.team.hotelbooking.payment.application.command;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateContractCommand(
    UUID hotelId,
    String contractCode,
    UUID ownerUserId,
    BigDecimal commissionRate,
    HotelContract.CommissionBasis commissionBasis,
    Instant startsAt,
    Instant endsAt,
    HotelContract.ContractStatus status,
    UUID currentAdminUserId) {}
