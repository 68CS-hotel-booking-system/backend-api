package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.FinancialAccessHistoryResult;
import java.time.Instant;
import java.util.UUID;

public record FinancialAccessHistoryDto(
    UUID id,
    long version,
    boolean isGranted,
    String accessScope,
    String reason,
    UUID changedByUserId,
    Instant changedAt) {

  public static FinancialAccessHistoryDto from(FinancialAccessHistoryResult result) {
    return new FinancialAccessHistoryDto(
        result.id(),
        result.version(),
        result.granted(),
        result.accessScope(),
        result.reason(),
        result.changedByUserId(),
        result.changedAt());
  }
}
