package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.port.*;
import com.team.hotelbooking.payment.domain.model.Refund;
import com.team.hotelbooking.payment.domain.repository.RefundRepository;
import java.time.Instant;
import java.util.UUID;

public class RefundRegistrationService implements RefundRegistrationPort {
  private final RefundRepository refunds;

  public RefundRegistrationService(RefundRepository refunds) {
    this.refunds = refunds;
  }

  public Refund register(
      UUID paymentId,
      PaymentBookingPort.BookingPaymentSnapshot b,
      long amount,
      String currency,
      String reason,
      String note,
      Instant now) {
    // Bên gọi giữ khóa booking rồi payment; khóa và ghi nhận tiền/hoàn tiền cùng transaction.
    return refunds
        .findByPaymentId(paymentId)
        .orElseGet(
            () ->
                refunds.add(
                    Refund.pending(
                        paymentId,
                        b.bookingId(),
                        b.customerId(),
                        b.hotelId(),
                        b.bookingCode(),
                        b.bookerName(),
                        b.bookerEmail(),
                        b.bookerPhone(),
                        amount,
                        currency,
                        reason,
                        note,
                        now)));
  }
}
