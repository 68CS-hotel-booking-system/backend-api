package com.team.hotelbooking.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Dữ liệu đã đối soát và nghĩa vụ hoàn tiền do Payment quản lý. */
public interface BookingCancellationPaymentGateway {

  Optional<CapturedPayment> findCapturedPayment(UUID bookingId);

  Optional<CancellationSettlement> findSettlement(UUID bookingId);

  void recordSettlement(UUID bookingId, CancellationSettlement settlement, Instant now);

  record CapturedPayment(long amount, String currency, BigDecimal commissionRate) {}

  record CancellationSettlement(
      long cancellationFeeAmount,
      long refundAmount,
      long commissionAmount,
      RefundStatus refundStatus) {}

  enum RefundStatus {
    NOT_REQUIRED,
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
  }
}
