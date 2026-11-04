package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.FinancialAccessDetailResult;
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
    List<FinancialAccessHistoryDto> history) {

  public static FinancialAccessDetailResponse from(FinancialAccessDetailResult result) {
    List<FinancialAccessHistoryDto> historyList =
        result.history().stream().map(FinancialAccessHistoryDto::from).toList();

    return new FinancialAccessDetailResponse(
        result.id(),
        result.hotelId(),
        result.userId(),
        result.userName(),
        result.userEmail(),
        result.granted(),
        result.accessScope(),
        result.reason(),
        result.grantedByUserId(),
        result.grantedAt(),
        result.version(),
        historyList);
  }
}
