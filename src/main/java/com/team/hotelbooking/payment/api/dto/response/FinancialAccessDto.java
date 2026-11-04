package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.FinancialAccessResult;
import java.time.Instant;
import java.util.UUID;

public record FinancialAccessDto(
    UUID id,
    UUID hotelId,
    UUID userId,
    boolean isGranted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version) {

  public static FinancialAccessDto from(FinancialAccessResult result) {
    return new FinancialAccessDto(
        result.id(),
        result.hotelId(),
        result.userId(),
        result.granted(),
        result.accessScope(),
        result.reason(),
        result.grantedByUserId(),
        result.grantedAt(),
        result.version());
  }
}
