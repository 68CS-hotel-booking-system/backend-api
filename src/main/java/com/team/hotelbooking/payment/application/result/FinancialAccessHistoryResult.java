package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.domain.model.FinancialAccessHistory;
import java.time.Instant;
import java.util.UUID;

public record FinancialAccessHistoryResult(
    UUID id,
    UUID financialAccessId,
    UUID hotelId,
    UUID userId,
    boolean granted,
    String accessScope,
    String reason,
    long version,
    UUID changedByUserId,
    Instant changedAt) {

  public static FinancialAccessHistoryResult from(FinancialAccessHistory domain) {
    return new FinancialAccessHistoryResult(
        domain.id(),
        domain.financialAccessId(),
        domain.hotelId(),
        domain.userId(),
        domain.granted(),
        domain.accessScope(),
        domain.reason(),
        domain.version(),
        domain.changedByUserId(),
        domain.changedAt());
  }
}
