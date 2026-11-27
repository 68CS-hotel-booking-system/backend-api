package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.BookingVoucherPort;
import com.team.hotelbooking.booking.domain.exception.*;
import com.team.hotelbooking.pricing.VoucherDirectory;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingVoucherAdapter implements BookingVoucherPort {
  private final VoucherDirectory vouchers;

  public void reserve(
      UUID hotel, String code, UUID booking, long room, long discount, Instant expires) {
    try {
      vouchers.reserve(hotel, code, booking, room, discount, expires);
    } catch (VoucherDirectory.VoucherUnavailableException e) {
      throw new BookingException(BookingErrorCode.VOUCHER_UNAVAILABLE, e.getMessage());
    }
  }

  public String bookingCode(UUID booking) {
    return vouchers.bookingCode(booking);
  }
}
