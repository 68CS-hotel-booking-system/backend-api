package com.team.hotelbooking.payment.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialAccessDetailResponse(
    UUID id,
    UUID hotelId,
    UUID userId,
    String userName,
    String userEmail,
    boolean isGranted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long currentVersion,
    List<FinancialAccessHistoryDto> history) {}
