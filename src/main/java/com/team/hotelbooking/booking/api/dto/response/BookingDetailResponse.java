package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BookingDetailResponse(
    UUID id,
    String bookingCode,
    UUID customerId,
    UUID hotelId,
    ContactResponse booker,
    ContactResponse guest,
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
    List<BookingItemResponse> items,
    PaymentHandoffResponse paymentHandoff) {

  public record ContactResponse(String fullName, String email, String phone) {
    public static ContactResponse from(BookingDetailResult.ContactResult contact) {
      return new ContactResponse(contact.fullName(), contact.email(), contact.phone());
    }
  }

  public record BookingItemResponse(
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
      List<NightlyPriceResponse> nightlyPrices) {

    public static BookingItemResponse from(BookingDetailResult.BookingItemResult item) {
      return new BookingItemResponse(
          item.id(),
          item.roomTypeId(),
          item.roomTypeName(),
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
          item.nightlyPrices().stream().map(NightlyPriceResponse::from).toList());
    }
  }

  public record NightlyPriceResponse(LocalDate stayDate, long unitPrice) {
    public static NightlyPriceResponse from(BookingDetailResult.NightlyPriceResult np) {
      return new NightlyPriceResponse(np.stayDate(), np.unitPrice());
    }
  }

  public record PaymentHandoffResponse(
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
      Instant holdExpiresAt) {

    public static PaymentHandoffResponse from(BookingDetailResult.PaymentHandoffResult handoff) {
      return new PaymentHandoffResponse(
          handoff.bookingId(),
          handoff.bookingCode(),
          handoff.hotelId(),
          handoff.customerId(),
          handoff.roomSubtotalAmount(),
          handoff.surchargeAmount(),
          handoff.subtotalAmount(),
          handoff.discountAmount(),
          handoff.payableAmount(),
          handoff.currency(),
          handoff.bookingStatus(),
          handoff.holdExpiresAt());
    }
  }

  public static BookingDetailResponse from(BookingDetailResult result) {
    return new BookingDetailResponse(
        result.id(),
        result.bookingCode(),
        result.customerId(),
        result.hotelId(),
        ContactResponse.from(result.booker()),
        ContactResponse.from(result.guest()),
        result.checkIn(),
        result.checkOut(),
        result.days(),
        result.nights(),
        result.standardCheckInTime(),
        result.standardCheckOutTime(),
        result.expectedCheckInTime(),
        result.expectedCheckOutTime(),
        result.adults(),
        result.children(),
        result.status(),
        result.currency(),
        result.roomSubtotalAmount(),
        result.earlyCheckInSurchargeAmount(),
        result.lateCheckOutSurchargeAmount(),
        result.extraGuestSurchargeAmount(),
        result.surchargeAmount(),
        result.subtotalAmount(),
        result.discountAmount(),
        result.totalAmount(),
        result.expiresAt(),
        result.createdAt(),
        result.updatedAt(),
        result.items().stream().map(BookingItemResponse::from).toList(),
        PaymentHandoffResponse.from(result.paymentHandoff()));
  }
}
