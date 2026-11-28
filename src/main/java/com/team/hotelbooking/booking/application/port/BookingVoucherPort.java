package com.team.hotelbooking.booking.application.port;

import java.time.Instant;
import java.util.UUID;

public interface BookingVoucherPort {
  void reserve(
      UUID hotel, String code, UUID booking, long roomAmount, long discount, Instant expiresAt);

  String bookingCode(UUID booking);

  BookingVoucherPort NONE =
      new BookingVoucherPort() {
        public void reserve(UUID h, String c, UUID b, long a, long d, Instant e) {
          if (c != null && !c.isBlank())
            throw new IllegalStateException("Voucher port is required");
        }

        public String bookingCode(UUID b) {
          return null;
        }
      };
}
