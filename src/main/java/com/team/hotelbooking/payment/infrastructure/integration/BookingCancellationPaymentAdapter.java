package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCancellationPaymentPolicy;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCancellationPaymentAdapter implements BookingCancellationPaymentPolicy {
  private final JdbcTemplate jdbc;
  private final com.team.hotelbooking.payment.infrastructure.persistence.repository
          .PaymentTransactionJpaRepository
      transactions;
  private final com.team.hotelbooking.payment.domain.repository.RefundRepository refunds;
  private final com.team.hotelbooking.payment.application.port.RefundRegistrationPort registration;
  private final com.team.hotelbooking.payment.application.port.PaymentBookingPort bookings;

  public long paidAmount(UUID bookingId) {
    return transactions
        .findAllByBookingIdAndStatusOrderByIdAsc(
            bookingId,
            com.team.hotelbooking.payment.infrastructure.persistence.entity
                .PaymentTransactionJpaEntity.Status.SUCCEEDED)
        .stream()
        .mapToLong(p -> p.getAmountVnd())
        .reduce(0, Math::addExact);
  }

  public RefundSummary refundSummary(UUID bookingId) {
    var list = refunds.findByBookingId(bookingId);
    long amount = list.stream().mapToLong(r -> r.amount()).reduce(0, Math::addExact);
    String status =
        list.isEmpty()
            ? "NOT_REQUIRED"
            : list.stream()
                    .allMatch(
                        r ->
                            r.status()
                                == com.team.hotelbooking.payment.domain.model.Refund.Status
                                    .COMPLETED)
                ? "COMPLETED"
                : list.stream()
                        .anyMatch(
                            r ->
                                r.status()
                                        == com.team.hotelbooking.payment.domain.model.Refund.Status
                                            .PROCESSING
                                    || r.status()
                                        == com.team.hotelbooking.payment.domain.model.Refund.Status
                                            .COMPLETED)
                    ? "PROCESSING"
                    : "PENDING";
    return new RefundSummary(amount, status);
  }

  public RefundSummary registerPaidCancellation(UUID bookingId, String reason, Instant now) {
    // The caller already holds the booking lock, shared with VNPay and check-in.
    var booking = bookings.findBookingForPayment(bookingId).orElseThrow();
    for (var payment :
        transactions.findAllByBookingIdAndStatusOrderByIdAsc(
            bookingId,
            com.team.hotelbooking.payment.infrastructure.persistence.entity
                .PaymentTransactionJpaEntity.Status.SUCCEEDED)) {
      registration.register(
          payment.getId(),
          booking,
          payment.getAmountVnd(),
          payment.getCurrency(),
          "CUSTOMER_CANCELLATION",
          reason,
          now);
    }
    return refundSummary(bookingId);
  }

  @Override
  public boolean hasPaymentRisk(UUID bookingId, Instant now) {
    return jdbc.queryForObject(
        """
        SELECT EXISTS (SELECT 1 FROM payment_transactions WHERE booking_id = ?
          AND (status = 'SUCCEEDED' OR (status = 'PENDING' AND expires_at > ?)))
        """,
        Boolean.class,
        bookingId,
        java.sql.Timestamp.from(now));
  }
}
