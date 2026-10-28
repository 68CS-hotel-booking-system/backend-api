package com.team.hotelbooking.payment.application.command;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public record GetContractsQuery(
    UUID hotelId,
    HotelContract.ContractStatus status,
    UUID requestingUserId,
    boolean isAdmin,
    Pageable pageable) {}
