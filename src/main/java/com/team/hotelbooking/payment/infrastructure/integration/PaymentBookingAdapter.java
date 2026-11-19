package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.payment.application.port.PaymentBookingPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentBookingAdapter implements PaymentBookingPort {

  private final BookingCheckoutDirectory bookingCheckoutDirectory;

  @Override
  public Optional<BookingPaymentSnapshot> lockBookingForPayment(UUID bookingId) {
    return bookingCheckoutDirectory.lockById(bookingId).map(this::snapshot);
  }

  @Override
  public boolean confirmPayment(UUID bookingId, java.time.Instant now) {
    return bookingCheckoutDirectory.confirmPayment(bookingId, now);
  }

  @Override
  public Optional<BookingPaymentSnapshot> findBookingForPayment(UUID bookingId) {
    return bookingCheckoutDirectory.findById(bookingId).map(this::snapshot);
  }

  private BookingPaymentSnapshot snapshot(BookingCheckoutDirectory.BookingCheckoutSnapshot b) {
    return new BookingPaymentSnapshot(
        b.bookingId(),
        b.bookingCode(),
        b.customerId(),
        b.hotelId(),
        b.booker().fullName(),
        b.booker().email(),
        b.booker().phone(),
        b.guest().fullName(),
        b.guest().email(),
        b.guest().phone(),
        b.checkIn(),
        b.checkOut(),
        b.days(),
        b.nights(),
        b.expectedCheckInTime(),
        b.expectedCheckOutTime(),
        b.roomSubtotalAmount(),
        b.surchargeAmount(),
        b.subtotalAmount(),
        b.discountAmount(),
        b.totalAmount(),
        b.currency(),
        b.status(),
        b.expiresAt());
  }
}
