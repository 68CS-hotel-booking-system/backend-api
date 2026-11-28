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

public class CancelBookingService
    implements CancelBookingUseCase,
        com.team.hotelbooking.booking.application.usecase.GetCancellationQuoteUseCase {
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

  private Booking owned(CancelBookingCommand command, boolean lock) {
    if (command == null || command.customerId() == null || command.bookingId() == null)
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    if (!accounts
        .findById(command.customerId())
        .map(BookingIdentityPort.AccountSnapshot::active)
        .orElse(false)) throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    return (lock ? bookings.lockById(command.bookingId()) : bookings.findById(command.bookingId()))
        .filter(value -> value.customerId().equals(command.customerId()))
        .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
  }

  private java.time.Instant cutoff(Booking booking) {
    return booking
        .checkIn()
        .atTime(booking.expectedCheckInTime())
        .atZone(com.team.hotelbooking.shared.time.BusinessTime.ZONE)
        .toInstant();
  }

  public com.team.hotelbooking.booking.application.result.CancellationQuoteResult quote(
      CancelBookingCommand command) {
    var booking = owned(command, false);
    var now = clock.instant();
    long amount =
        booking.status() == Booking.Status.CONFIRMED ? payments.paidAmount(booking.id()) : 0;
    boolean allowed =
        booking.status() == Booking.Status.CONFIRMED
            ? now.isBefore(cutoff(booking)) && amount > 0
            : booking.status() == Booking.Status.PENDING_PAYMENT
                && !payments.hasPaymentRisk(booking.id(), now);
    String message =
        allowed
            ? (amount > 0
                ? "Hủy ngay và hoàn 100% số tiền đã thu. Admin sẽ xử lý hoàn tiền."
                : "Hủy đơn chưa thanh toán, không mất phí.")
            : booking.status() == Booking.Status.CONFIRMED
                ? "Chỉ được hủy đơn đã thanh toán trước giờ nhận phòng và khi đã ghi nhận tiền thanh toán."
                : "Trạng thái đơn hoặc giao dịch thanh toán không cho phép hủy.";
    return new com.team.hotelbooking.booking.application.result.CancellationQuoteResult(
        allowed, message, booking.version(), amount, booking.currency(), cutoff(booking));
  }

  @Override
  public CancelBookingResult execute(CancelBookingCommand command) {
    var booking = owned(command, true);
    var now = clock.instant();
    if (command.reason() != null && command.reason().length() > 1000)
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    if (booking.status() == Booking.Status.CONFIRMED) {
      long amount = payments.paidAmount(booking.id());
      if (!now.isBefore(cutoff(booking)) || amount <= 0)
        throw new BookingException(BookingErrorCode.BOOKING_NOT_CANCELLABLE);
      if (command.expectedVersion() == null
          || command.expectedVersion() != booking.version()
          || command.expectedRefundAmount() == null
          || command.expectedRefundAmount() != amount)
        throw new BookingException(
            BookingErrorCode.BOOKING_NOT_CANCELLABLE,
            "Thông tin hủy đơn đã thay đổi. Vui lòng tải lại và xác nhận số tiền hoàn.");
      var refund =
          payments.registerPaidCancellation(
              booking.id(), command.reason() == null ? null : command.reason().trim(), now);
      inventory.release(booking, false);
      bookings.cancelConfirmed(booking.id(), now, command.customerId(), command.reason());
      return new CancelBookingResult(
          "CANCELLED", booking.currency(), refund.amount(), refund.status());
    }
    if (booking.status() != Booking.Status.PENDING_PAYMENT
        && booking.status() != Booking.Status.CANCELLED
        && booking.status() != Booking.Status.EXPIRED)
      throw new BookingException(BookingErrorCode.BOOKING_NOT_CANCELLABLE);
    if (booking.status() == Booking.Status.CANCELLED
        || booking.status() == Booking.Status.EXPIRED) {
      var refund = payments.refundSummary(booking.id());
      if (refund != null && refund.amount() > 0)
        return new CancelBookingResult(
            booking.status().name(), booking.currency(), refund.amount(), refund.status());
    }
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
