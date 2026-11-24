package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.BookingCancellationPaymentPolicy;
import com.team.hotelbooking.booking.application.command.CancelBookingCommand;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;
import com.team.hotelbooking.booking.application.usecase.CancelBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.time.Clock;

public class CancelBookingService implements CancelBookingUseCase {
  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final BookingInventoryPort inventory;
  private final BookingCancellationPaymentPolicy payments;
  private final Clock clock;

  public CancelBookingService(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingInventoryPort inventory,
      BookingCancellationPaymentPolicy payments,
      Clock clock) {
    this.bookings = bookings;
    this.accounts = accounts;
    this.inventory = inventory;
    this.payments = payments;
    this.clock = clock;
  }

  @Override
  public CancelBookingResult execute(CancelBookingCommand command) {
    if (command == null || command.customerId() == null || command.bookingId() == null)
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    if (!accounts
        .findById(command.customerId())
        .map(BookingIdentityPort.AccountSnapshot::active)
        .orElse(false)) throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    var booking =
        bookings
            .lockById(command.bookingId())
            .filter(value -> value.customerId().equals(command.customerId()))
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
    if (booking.status() != Booking.Status.PENDING_PAYMENT
        && booking.status() != Booking.Status.CANCELLED
        && booking.status() != Booking.Status.EXPIRED)
      throw new BookingException(BookingErrorCode.BOOKING_NOT_CANCELLABLE);
    var now = clock.instant();
    if (payments.hasPaymentRisk(booking.id(), now))
      throw new BookingException(BookingErrorCode.BOOKING_PAYMENT_IN_PROGRESS);
    if (booking.status() == Booking.Status.CANCELLED || booking.status() == Booking.Status.EXPIRED)
      return new CancelBookingResult(booking.status().name(), booking.currency());
    var target =
        booking.expiresAt() != null && !booking.expiresAt().isAfter(now)
            ? Booking.Status.EXPIRED
            : Booking.Status.CANCELLED;
    inventory.release(booking, target == Booking.Status.EXPIRED);
    bookings.cancelPending(booking.id(), target, now, command.customerId());
    return new CancelBookingResult(target.name(), booking.currency());
  }
}
