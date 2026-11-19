package com.team.hotelbooking.payment.application.result;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity;
import java.time.Instant;
import java.util.UUID;

public record PaymentResult(
    UUID id,
    UUID bookingId,
    long amountVnd,
    String currency,
    String status,
    String paymentUrl,
    String qrContent,
    Instant expiresAt,
    boolean bookingConfirmed,
    boolean expired) {
  public static PaymentResult from(PaymentTransactionJpaEntity payment, Instant now) {
    String url =
        payment.getStatus() == PaymentTransactionJpaEntity.Status.PENDING
                && payment.getExpiresAt().isAfter(now)
            ? payment.getPaymentUrl()
            : null;
    return new PaymentResult(
        payment.getId(),
        payment.getBookingId(),
        payment.getAmountVnd(),
        payment.getCurrency(),
        payment.getStatus().name(),
        url,
        url != null ? payment.getQrContent() : null,
        payment.getExpiresAt(),
        payment.isBookingConfirmed(),
        !payment.getExpiresAt().isAfter(now));
  }
}
