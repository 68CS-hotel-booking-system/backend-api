package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.domain.model.FinancialAccess;
import java.time.Instant;
import java.util.UUID;

public record FinancialAccessResult(
    UUID id,
    UUID hotelId,
    UUID userId,
    boolean granted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version) {

  public static FinancialAccessResult from(FinancialAccess domain) {
    return new FinancialAccessResult(
        domain.id(),
        domain.hotelId(),
        domain.userId(),
        domain.granted(),
        domain.accessScope(),
        domain.reason(),
        domain.grantedByUserId(),
        domain.grantedAt(),
        domain.version());
  }
}
