package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.BookingCancellationPaymentGateway;
import com.team.hotelbooking.booking.BookingCancellationPaymentGateway.CancellationSettlement;
import com.team.hotelbooking.booking.BookingCancellationPaymentGateway.CapturedPayment;
import com.team.hotelbooking.booking.BookingCancellationPaymentGateway.RefundStatus;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;
import com.team.hotelbooking.booking.application.usecase.CancelBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Booking.Status;
import com.team.hotelbooking.booking.domain.model.CancellationAmounts;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class CancelBookingService implements CancelBookingUseCase {

  private static final CancellationSettlement NO_SETTLEMENT =
      new CancellationSettlement(0, 0, 0, RefundStatus.NOT_REQUIRED);

  private final BookingRepository bookings;
  private final BookingInventoryPort inventory;
  private final VoucherRepository vouchers;
  private final BookingCancellationPaymentGateway payments;
  private final Clock clock;

  public CancelBookingService(
      BookingRepository bookings,
      BookingInventoryPort inventory,
      VoucherRepository vouchers,
      BookingCancellationPaymentGateway payments,
      Clock clock) {
    this.bookings = bookings;
    this.inventory = inventory;
    this.vouchers = vouchers;
    this.payments = payments;
    this.clock = clock;
  }

  @Override
  public CancelBookingResult execute(UUID customerId, UUID bookingId) {
    Booking booking =
        bookings
            .lockById(bookingId)
            .filter(found -> found.customerId().equals(customerId))
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
    if (booking.status() == Status.CANCELLED) {
      return cancelledResult(booking);
    }
    if (booking.status() == Status.EXPIRED) {
      return result(booking, NO_SETTLEMENT);
    }
    if (booking.status() == Status.CONFIRMED) {
      return cancelConfirmed(booking, customerId, "Khách hủy đơn.", false);
    }
    if (booking.status() != Status.PENDING_PAYMENT) {
      throw new BookingException(BookingErrorCode.BOOKING_NOT_CANCELLABLE);
    }
    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    close(bookingId, customerId, now, hasExpired(booking, now));
    return result(bookings.findById(bookingId).orElseThrow(), NO_SETTLEMENT);
  }

  public CancelBookingResult cancelByHotel(
      UUID hotelId, UUID bookingId, UUID actorId, String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 255) {
      throw new BookingException(
          BookingErrorCode.VALIDATION_FAILED, "Lý do hủy phải có từ 1 đến 255 ký tự.");
    }
    Booking booking =
        bookings
            .lockById(bookingId)
            .filter(found -> found.hotelId().equals(hotelId))
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
    if (booking.status() == Status.CANCELLED) {
      return cancelledResult(booking);
    }
    if (booking.status() != Status.CONFIRMED) {
      throw new BookingException(
          BookingErrorCode.BOOKING_NOT_CANCELLABLE,
          "Khách sạn chỉ có thể hủy đơn đã xác nhận và chưa nhận phòng.");
    }
    return cancelConfirmed(booking, actorId, reason.strip(), true);
  }

  private CancelBookingResult cancelConfirmed(
      Booking booking, UUID actorId, String reason, boolean cancelledByHotel) {
    CapturedPayment captured =
        payments
            .findCapturedPayment(booking.id())
            .filter(
                payment ->
                    payment.amount() == booking.totalAmount()
                        && booking.currency().equals(payment.currency()))
            .orElseThrow(
                () -> new BookingException(BookingErrorCode.CANCELLATION_PAYMENT_UNVERIFIED));
    Instant now = clock.instant();
    var amounts =
        CancellationAmounts.calculate(
            booking, now, cancelledByHotel, captured.amount(), captured.commissionRate());
    Instant recordedAt = now.truncatedTo(ChronoUnit.MICROS);
    var settlement =
        new CancellationSettlement(
            amounts.fee(),
            amounts.refund(),
            amounts.commission(),
            amounts.refund() > 0 ? RefundStatus.PENDING : RefundStatus.NOT_REQUIRED);
    payments.recordSettlement(booking.id(), settlement, recordedAt);
    bookings.cancelConfirmed(booking.id(), actorId, reason, cancelledByHotel, recordedAt);
    inventory.releaseConfirmed(booking.id(), actorId, recordedAt);
    return result(bookings.findById(booking.id()).orElseThrow(), settlement);
  }

  private CancelBookingResult cancelledResult(Booking booking) {
    return result(booking, payments.findSettlement(booking.id()).orElse(NO_SETTLEMENT));
  }

  private static CancelBookingResult result(Booking booking, CancellationSettlement settlement) {
    return new CancelBookingResult(
        BookingDetailResult.from(booking),
        settlement.cancellationFeeAmount(),
        settlement.refundAmount(),
        settlement.refundStatus().name());
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
