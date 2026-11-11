package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.util.UUID;

// actorUserId và admin lấy từ principal; quyền theo hotel kiểm trong use case.
public record SetRoomRatesCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
    UUID roomTypeId,
    LocalDate startDate,
    LocalDate endDateExclusive,
    Long amount,
    String currency) {}
