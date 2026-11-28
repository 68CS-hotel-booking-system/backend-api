package com.team.hotelbooking.pricing;

import java.time.Instant;
import java.util.UUID;

public interface VoucherDirectory {
  long discount(UUID hotel, String code, long roomAmount);

  void reserve(
      UUID hotel, String code, UUID booking, long roomAmount, long discount, Instant expiresAt);

  void lockForPayment(UUID booking);

  void consume(UUID booking);

  void release(UUID booking);

  String bookingCode(UUID booking);

  final class VoucherUnavailableException extends RuntimeException {
    public VoucherUnavailableException(String message) {
      super(message);
    }
  }
}
