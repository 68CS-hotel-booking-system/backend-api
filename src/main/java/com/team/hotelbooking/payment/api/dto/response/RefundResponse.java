package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.RefundResult;
import java.time.Instant;
import java.util.*;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    UUID bookingId,
    String bookingCode,
    UUID hotelId,
    String hotelName,
    String bookerName,
    String bookerEmail,
    String bookerPhone,
    long amount,
    String currency,
    String reason,
    String customerNote,
    String status,
    String settlementReference,
    String note,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    long version,
    boolean canProcess,
    List<Action> history) {
  public record Action(
      UUID id,
      String fromStatus,
      String toStatus,
      String note,
      String settlementReference,
      Instant createdAt,
      long version) {}

  public static RefundResponse from(RefundResult r) {
    return new RefundResponse(
        r.id(),
        r.paymentId(),
        r.bookingId(),
        r.bookingCode(),
        r.hotelId(),
        r.hotelName(),
        r.bookerName(),
        r.bookerEmail(),
        r.bookerPhone(),
        r.amount(),
        r.currency(),
        r.reason(),
        r.customerNote(),
        r.status(),
        r.settlementReference(),
        r.note(),
        r.createdAt(),
        r.updatedAt(),
        r.completedAt(),
        r.version(),
        r.canProcess(),
        r.history().stream()
            .map(
                a ->
                    new Action(
                        a.id(),
                        a.fromStatus(),
                        a.toStatus(),
                        a.note(),
                        a.settlementReference(),
                        a.createdAt(),
                        a.version()))
            .toList());
  }
}
