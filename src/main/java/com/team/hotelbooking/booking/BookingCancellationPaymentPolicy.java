package com.team.hotelbooking.booking;

import java.time.Instant;
import java.util.UUID;

public interface BookingCancellationPaymentPolicy {
  boolean hasPaymentRisk(UUID bookingId, Instant now);
}
