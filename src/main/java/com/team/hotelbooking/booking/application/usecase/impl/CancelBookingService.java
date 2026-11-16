package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.usecase.CancelBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Booking.Status;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CancelBookingService implements CancelBookingUseCase {

  private final BookingRepository bookings;
  private final BookingInventoryPort inventory;
  private final VoucherRepository vouchers;
  private final Clock clock;

  public CancelBookingService(
      BookingRepository bookings,
      BookingInventoryPort inventory,
      VoucherRepository vouchers,
      Clock clock) {
    this.bookings = bookings;
    this.inventory = inventory;
    this.vouchers = vouchers;
    this.clock = clock;
  }

  @Override
  public BookingDetailResult execute(UUID customerId, UUID bookingId) {
    Booking booking =
        bookings
            .lockById(bookingId)
            .filter(found -> found.customerId().equals(customerId))
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
    if (booking.status() == Status.CANCELLED || booking.status() == Status.EXPIRED) {
      return BookingDetailResult.from(booking);
    }
    if (booking.status() != Status.PENDING_PAYMENT) {
      throw new BookingException(BookingErrorCode.BOOKING_NOT_CANCELLABLE);
    }
    Instant now = clock.instant();
    close(bookingId, customerId, now, hasExpired(booking, now));
    return BookingDetailResult.from(bookings.findById(bookingId).orElseThrow());
  }

  public void expire(UUID bookingId) {
    bookings
        .lockById(bookingId)
        .ifPresent(
            booking -> {
              Instant now = clock.instant();
              if (booking.status() == Status.PENDING_PAYMENT && hasExpired(booking, now)) {
                close(bookingId, null, now, true);
              }
            });
  }

  private static boolean hasExpired(Booking booking, Instant now) {
    return booking.expiresAt() != null && !booking.expiresAt().isAfter(now);
  }

  private void close(UUID bookingId, UUID actorId, Instant now, boolean expired) {
    bookings.closePending(bookingId, expired ? Status.EXPIRED : Status.CANCELLED, actorId, now);
    inventory.release(bookingId, expired);
    vouchers.release(bookingId, now);
  }
}
