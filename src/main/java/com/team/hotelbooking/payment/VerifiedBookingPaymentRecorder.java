package com.team.hotelbooking.payment;

import java.time.Instant;
import java.util.UUID;

/** Điểm ghi nội bộ cho luồng xác nhận đơn, sau khi cổng đã xác thực khoản thu. */
public interface VerifiedBookingPaymentRecorder {

  void record(VerifiedCapture capture);

  record VerifiedCapture(
      UUID bookingId,
      String provider,
      String transactionReference,
      long amount,
      String currency,
      Instant verifiedAt) {}
}
