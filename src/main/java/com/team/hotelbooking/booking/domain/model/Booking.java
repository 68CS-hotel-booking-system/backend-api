package com.team.hotelbooking.booking.domain.model;

import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record Booking(
    UUID id,
    String bookingCode,
    UUID customerId,
    UUID hotelId,
    ContactSnapshot booker,
    ContactSnapshot guest,
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
    Status status,
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
    long version,
    List<BookingItem> items) {

  public enum Status {
    PENDING_PAYMENT,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    EXPIRED,
    NO_SHOW
  }

  public record ContactSnapshot(String fullName, String email, String phone) {
    public static ContactSnapshot create(
        String fullName, String email, String phone, String roleLabel) {
      String normalizedName = fullName == null ? "" : fullName.trim();
      if (normalizedName.isBlank() || normalizedName.length() > 100) {
        throw new BookingException(
            BookingErrorCode.VALIDATION_FAILED,
            "Họ tên " + roleLabel + " phải có từ 1 đến 100 ký tự.");
      }

      String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
      if (normalizedEmail.isBlank()
          || normalizedEmail.length() > 255
          || !normalizedEmail.contains("@")) {
        throw new BookingException(
            BookingErrorCode.VALIDATION_FAILED, "Email " + roleLabel + " không hợp lệ.");
      }

      String normalizedPhone = phone == null ? "" : phone.trim();
      if (normalizedPhone.isBlank() || normalizedPhone.length() > 20) {
        throw new BookingException(
            BookingErrorCode.VALIDATION_FAILED,
            "Số điện thoại " + roleLabel + " phải có từ 1 đến 20 ký tự.");
      }

      return new ContactSnapshot(normalizedName, normalizedEmail, normalizedPhone);
    }
  }

  public record BookingItem(
      UUID id,
      UUID roomTypeId,
      String roomTypeNameSnapshot,
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
      List<BookingNightPrice> nightlyPrices) {}

  public record BookingNightPrice(LocalDate stayDate, long unitPrice) {}

  public static Booking createPendingPayment(
      UUID id,
      String bookingCode,
      UUID customerId,
      UUID hotelId,
      ContactSnapshot booker,
      ContactSnapshot guest,
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
      Instant now,
      List<BookingItem> items) {

    if (id == null
        || bookingCode == null
        || bookingCode.isBlank()
        || customerId == null
        || hotelId == null
        || booker == null
        || guest == null
        || checkIn == null
        || checkOut == null
        || expiresAt == null
        || now == null
        || items == null
        || items.isEmpty()) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    if (nights < 1 || days != nights + 1 || !checkOut.isAfter(checkIn)) {
      throw new BookingException(BookingErrorCode.INVALID_DATE_RANGE);
    }

    if (roomSubtotalAmount < 0
        || surchargeAmount < 0
        || subtotalAmount != roomSubtotalAmount + surchargeAmount
        || discountAmount < 0
        || discountAmount > subtotalAmount
        || totalAmount != subtotalAmount - discountAmount) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED, "Tổng tiền đơn không hợp lệ.");
    }

    return new Booking(
        id,
        bookingCode.trim(),
        customerId,
        hotelId,
        booker,
        guest,
        checkIn,
        checkOut,
        days,
        nights,
        standardCheckInTime,
        standardCheckOutTime,
        expectedCheckInTime,
        expectedCheckOutTime,
        adults,
        children,
        Status.PENDING_PAYMENT,
        currency == null ? "VND" : currency,
        roomSubtotalAmount,
        earlyCheckInSurchargeAmount,
        lateCheckOutSurchargeAmount,
        extraGuestSurchargeAmount,
        surchargeAmount,
        subtotalAmount,
        discountAmount,
        totalAmount,
        expiresAt,
        now,
        now,
        0L,
        List.copyOf(items));
  }

  public static Booking rehydrate(
      UUID id,
      String bookingCode,
      UUID customerId,
      UUID hotelId,
      ContactSnapshot booker,
      ContactSnapshot guest,
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
      Status status,
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
      long version,
      List<BookingItem> items) {

    return new Booking(
        id,
        bookingCode,
        customerId,
        hotelId,
        booker,
        guest,
        checkIn,
        checkOut,
        days,
        nights,
        standardCheckInTime,
        standardCheckOutTime,
        expectedCheckInTime,
        expectedCheckOutTime,
        adults,
        children,
        status,
        currency,
        roomSubtotalAmount,
        earlyCheckInSurchargeAmount,
        lateCheckOutSurchargeAmount,
        extraGuestSurchargeAmount,
        surchargeAmount,
        subtotalAmount,
        discountAmount,
        totalAmount,
        expiresAt,
        createdAt,
        updatedAt,
        version,
        items == null ? List.of() : List.copyOf(items));
  }
}
