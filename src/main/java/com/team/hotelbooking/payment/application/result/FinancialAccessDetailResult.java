package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.domain.model.FinancialAccessDetail;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialAccessDetailResult(
    UUID id,
    UUID hotelId,
    UUID userId,
    String userName,
    String userEmail,
    boolean granted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version,
    List<FinancialAccessHistoryResult> history) {

  public static FinancialAccessDetailResult from(FinancialAccessDetail domain) {
    List<FinancialAccessHistoryResult> historyList =
        domain.history().stream().map(FinancialAccessHistoryResult::from).toList();

    return new FinancialAccessDetailResult(
        domain.id(),
        domain.hotelId(),
        domain.userId(),
        domain.userName(),
        domain.userEmail(),
        domain.granted(),
        domain.accessScope(),
        domain.reason(),
        domain.grantedByUserId(),
        domain.grantedAt(),
        domain.version(),
        historyList);
  }
}
