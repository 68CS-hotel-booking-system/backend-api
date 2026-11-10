package com.team.hotelbooking.booking.application.result;

import com.team.hotelbooking.booking.domain.model.Booking;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BookingDetailResult(
    UUID id,
    String bookingCode,
    UUID customerId,
    UUID hotelId,
    ContactResult booker,
    ContactResult guest,
    LocalDate checkIn,
    LocalDate checkOut,
    int days,
    int nights,
    LocalTime standardCheckInTime,
    LocalTime standardCheckOutTime,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    int adults,
    int children,
    String status,
    String currency,
    long roomSubtotalAmount,
    long earlyCheckInSurchargeAmount,
    long lateCheckOutSurchargeAmount,
    long extraGuestSurchargeAmount,
    long surchargeAmount,
    long subtotalAmount,
    long discountAmount,
    long totalAmount,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt,
    List<BookingItemResult> items,
    PaymentHandoffResult paymentHandoff) {

  public record ContactResult(String fullName, String email, String phone) {
    public static ContactResult from(Booking.ContactSnapshot snapshot) {
      return new ContactResult(snapshot.fullName(), snapshot.email(), snapshot.phone());
    }
  }

  public record BookingItemResult(
      UUID id,
      UUID roomTypeId,
      String roomTypeName,
      int quantity,
      int adults,
      int children,
      int extraGuests,
      long roomSubtotalAmount,
      long earlyCheckInSurchargeAmount,
      long lateCheckOutSurchargeAmount,
      long extraGuestSurchargeAmount,
      long surchargeAmount,
      long subtotalAmount,
      List<NightlyPriceResult> nightlyPrices) {

    public static BookingItemResult from(Booking.BookingItem item) {
      return new BookingItemResult(
          item.id(),
          item.roomTypeId(),
          item.roomTypeNameSnapshot(),
          item.quantity(),
          item.adults(),
          item.children(),
          item.extraGuests(),
          item.roomSubtotalAmount(),
          item.earlyCheckInSurchargeAmount(),
          item.lateCheckOutSurchargeAmount(),
          item.extraGuestSurchargeAmount(),
          item.surchargeAmount(),
          item.subtotalAmount(),
          item.nightlyPrices().stream().map(NightlyPriceResult::from).toList());
    }
  }

  public record NightlyPriceResult(LocalDate stayDate, long unitPrice) {
    public static NightlyPriceResult from(Booking.BookingNightPrice np) {
      return new NightlyPriceResult(np.stayDate(), np.unitPrice());
    }
  }

  public record PaymentHandoffResult(
      UUID bookingId,
      String bookingCode,
      UUID hotelId,
      UUID customerId,
      long roomSubtotalAmount,
      long surchargeAmount,
      long subtotalAmount,
      long discountAmount,
      long payableAmount,
      String currency,
      String bookingStatus,
      Instant holdExpiresAt) {}

  public static BookingDetailResult from(Booking booking) {
    return new BookingDetailResult(
        booking.id(),
        booking.bookingCode(),
        booking.customerId(),
        booking.hotelId(),
        ContactResult.from(booking.booker()),
        ContactResult.from(booking.guest()),
        booking.checkIn(),
        booking.checkOut(),
        booking.days(),
        booking.nights(),
        booking.standardCheckInTime(),
        booking.standardCheckOutTime(),
        booking.expectedCheckInTime(),
        booking.expectedCheckOutTime(),
        booking.adults(),
        booking.children(),
        booking.status().name(),
        booking.currency(),
        booking.roomSubtotalAmount(),
        booking.earlyCheckInSurchargeAmount(),
        booking.lateCheckOutSurchargeAmount(),
        booking.extraGuestSurchargeAmount(),
        booking.surchargeAmount(),
        booking.subtotalAmount(),
        booking.discountAmount(),
        booking.totalAmount(),
        booking.expiresAt(),
        booking.createdAt(),
        booking.updatedAt(),
        booking.items().stream().map(BookingItemResult::from).toList(),
        new PaymentHandoffResult(
            booking.id(),
            booking.bookingCode(),
            booking.hotelId(),
            booking.customerId(),
            booking.roomSubtotalAmount(),
            booking.surchargeAmount(),
            booking.subtotalAmount(),
            booking.discountAmount(),
            booking.totalAmount(),
            booking.currency(),
            booking.status().name(),
            booking.expiresAt()));
  }
}
