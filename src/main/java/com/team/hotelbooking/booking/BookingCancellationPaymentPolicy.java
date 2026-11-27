package com.team.hotelbooking.booking;

import java.time.Instant;
import java.util.UUID;

public interface BookingCancellationPaymentPolicy {
  boolean hasPaymentRisk(UUID bookingId, Instant now);

  long paidAmount(UUID bookingId);

  RefundSummary refundSummary(UUID bookingId);

  RefundSummary registerPaidCancellation(UUID bookingId, String reason, Instant now);

  record RefundSummary(long amount, String status) {}
}
