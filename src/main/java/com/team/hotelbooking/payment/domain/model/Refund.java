package com.team.hotelbooking.payment.domain.model;

import com.team.hotelbooking.payment.domain.exception.*;
import java.time.Instant;
import java.util.UUID;

public record Refund(
    UUID id,
    UUID paymentId,
    UUID bookingId,
    UUID customerId,
    UUID hotelId,
    String bookingCode,
    String bookerName,
    String bookerEmail,
    String bookerPhone,
    long amount,
    String currency,
    String reason,
    String customerNote,
    Status status,
    String settlementReference,
    String note,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    long version) {
  public enum Status {
    PENDING,
    PROCESSING,
    COMPLETED
  }

  public static Refund pending(
      UUID paymentId,
      UUID bookingId,
      UUID customerId,
      UUID hotelId,
      String bookingCode,
      String name,
      String email,
      String phone,
      long amount,
      String currency,
      String reason,
      String customerNote,
      Instant now) {
    if (amount <= 0 || !"VND".equals(currency))
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    return new Refund(
        UUID.randomUUID(),
        paymentId,
        bookingId,
        customerId,
        hotelId,
        bookingCode,
        name,
        email,
        phone,
        amount,
        currency,
        reason,
        customerNote,
        Status.PENDING,
        null,
        null,
        now,
        now,
        null,
        0);
  }

  public Refund process(Status next, String reference, String response, Instant now) {
    String text = response == null ? "" : response.trim();
    String ref = reference == null ? "" : reference.trim();
    if (text.isEmpty() || text.length() > 1000 || ref.length() > 100)
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    if (!(status == Status.PENDING && next == Status.PROCESSING
        || status == Status.PROCESSING && next == Status.COMPLETED))
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND_TRANSITION);
    if (next == Status.COMPLETED && ref.isEmpty())
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    return new Refund(
        id,
        paymentId,
        bookingId,
        customerId,
        hotelId,
        bookingCode,
        bookerName,
        bookerEmail,
        bookerPhone,
        amount,
        currency,
        reason,
        customerNote,
        next,
        next == Status.COMPLETED ? ref : null,
        text,
        createdAt,
        now,
        next == Status.COMPLETED ? now : null,
        version + 1);
  }
}
