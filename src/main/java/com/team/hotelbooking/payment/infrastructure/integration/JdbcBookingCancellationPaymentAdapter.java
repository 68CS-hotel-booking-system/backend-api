package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCancellationPaymentGateway;
import com.team.hotelbooking.payment.VerifiedBookingPaymentRecorder.VerifiedCapture;
import com.team.hotelbooking.payment.application.port.VerifiedBookingPaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcBookingCancellationPaymentAdapter
    implements BookingCancellationPaymentGateway, VerifiedBookingPaymentRepository {

  private final JdbcClient jdbc;

  @Override
  public Optional<CapturedPayment> findCapturedPayment(UUID bookingId) {
    return find(bookingId)
        .map(
            payment ->
                new CapturedPayment(
                    payment.amount(), payment.currency(), payment.commissionRate()));
  }

  @Override
  public Optional<PersistedCapture> find(UUID bookingId) {
    return jdbc.sql(
            """
        SELECT provider, transaction_reference, amount, currency, commission_rate
        FROM verified_booking_payments WHERE booking_id = :bookingId
        """)
        .param("bookingId", bookingId)
        .query(
            (row, index) ->
                new PersistedCapture(
                    row.getString(1),
                    row.getString(2),
                    row.getLong(3),
                    row.getString(4),
                    row.getBigDecimal(5)))
        .optional();
  }

  @Override
  public boolean addIfAbsent(VerifiedCapture capture, UUID contractId, BigDecimal commissionRate) {
    return jdbc.sql(
                """
        INSERT INTO verified_booking_payments
            (booking_id, provider, transaction_reference, amount, currency, verified_at, contract_id, commission_rate)
        VALUES (:bookingId, :provider, :reference, :amount, :currency, :verifiedAt, :contractId, :rate)
        ON CONFLICT (booking_id) DO NOTHING
        """)
            .param("bookingId", capture.bookingId())
            .param("provider", capture.provider())
            .param("reference", capture.transactionReference())
            .param("amount", capture.amount())
            .param("currency", capture.currency())
            .param("verifiedAt", capture.verifiedAt().atOffset(ZoneOffset.UTC))
            .param("contractId", contractId)
            .param("rate", commissionRate)
            .update()
        == 1;
  }

  @Override
  public Optional<CancellationSettlement> findSettlement(UUID bookingId) {
    return jdbc.sql(
            """
        SELECT cancellation_fee_amount, refund_amount, commission_amount, refund_status
        FROM booking_cancellation_settlements WHERE booking_id = :bookingId
        """)
        .param("bookingId", bookingId)
        .query(
            (row, index) ->
                new CancellationSettlement(
                    row.getLong(1),
                    row.getLong(2),
                    row.getLong(3),
                    RefundStatus.valueOf(row.getString(4))))
        .optional();
  }

  @Override
  public void recordSettlement(UUID bookingId, CancellationSettlement settlement, Instant now) {
    jdbc.sql(
            """
        INSERT INTO booking_cancellation_settlements
            (booking_id, cancellation_fee_amount, refund_amount, commission_amount, refund_status, created_at)
        VALUES (:bookingId, :fee, :refund, :commission, :status, :now)
        """)
        .param("bookingId", bookingId)
        .param("fee", settlement.cancellationFeeAmount())
        .param("refund", settlement.refundAmount())
        .param("commission", settlement.commissionAmount())
        .param("status", settlement.refundStatus().name())
        .param("now", now.atOffset(ZoneOffset.UTC))
        .update();
  }
}
