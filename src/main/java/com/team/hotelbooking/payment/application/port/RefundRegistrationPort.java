package com.team.hotelbooking.payment.application.port;

import com.team.hotelbooking.payment.domain.model.Refund;
import java.time.Instant;
import java.util.UUID;

public interface RefundRegistrationPort {
  Refund register(
      UUID paymentId,
      PaymentBookingPort.BookingPaymentSnapshot booking,
      long amount,
      String currency,
      String reason,
      String customerNote,
      Instant now);
}
