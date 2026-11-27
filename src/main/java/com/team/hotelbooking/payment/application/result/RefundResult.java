package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.domain.model.*;
import java.time.Instant;
import java.util.*;

public record RefundResult(
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
      long version) {
    public static Action from(RefundAction a) {
      return new Action(
          a.id(),
          a.fromStatus().name(),
          a.toStatus().name(),
          a.note(),
          a.settlementReference(),
          a.createdAt(),
          a.version());
    }
  }

  public static RefundResult from(
      Refund r, String hotelName, boolean canProcess, List<RefundAction> history) {
    return new RefundResult(
        r.id(),
        r.paymentId(),
        r.bookingId(),
        r.bookingCode(),
        r.hotelId(),
        hotelName,
        r.bookerName(),
        r.bookerEmail(),
        r.bookerPhone(),
        r.amount(),
        r.currency(),
        r.reason(),
        r.customerNote(),
        r.status().name(),
        r.settlementReference(),
        r.note(),
        r.createdAt(),
        r.updatedAt(),
        r.completedAt(),
        r.version(),
        canProcess,
        history.stream().map(Action::from).toList());
  }
}
