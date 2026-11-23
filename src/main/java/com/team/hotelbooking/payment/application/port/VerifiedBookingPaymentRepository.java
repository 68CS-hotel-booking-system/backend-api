package com.team.hotelbooking.payment.application.port;

import com.team.hotelbooking.payment.VerifiedBookingPaymentRecorder.VerifiedCapture;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface VerifiedBookingPaymentRepository {

  Optional<PersistedCapture> find(UUID bookingId);

  boolean addIfAbsent(VerifiedCapture capture, UUID contractId, BigDecimal commissionRate);

  record PersistedCapture(
      String provider,
      String transactionReference,
      long amount,
      String currency,
      BigDecimal commissionRate) {

    public boolean matches(VerifiedCapture capture) {
      return provider.equals(capture.provider())
          && transactionReference.equals(capture.transactionReference())
          && amount == capture.amount()
          && currency.equals(capture.currency());
    }
  }
}
