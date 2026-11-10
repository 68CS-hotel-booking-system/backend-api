package com.team.hotelbooking.pricing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface StayQuoteDirectory {

  StayQuoteSnapshot calculateQuote(StayQuoteRequest request);

  enum FailureReason {
    INVALID_DATE_RANGE,
    MAX_STAY_EXCEEDED,
    INVALID_QUOTE_REQUEST,
    HOTEL_NOT_FOUND,
    ROOM_TYPE_NOT_FOUND,
    INVALID_OCCUPANCY,
    RATE_NOT_AVAILABLE
  }

  final class StayQuoteCalculationException extends RuntimeException {
    private final FailureReason reason;

    public StayQuoteCalculationException(FailureReason reason, String message) {
      super(message);
      this.reason = reason;
    }

    public FailureReason reason() {
      return reason;
    }
  }

  record StayQuoteRequest(
      UUID hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      LocalTime expectedCheckInTime,
      LocalTime expectedCheckOutTime,
      List<StayQuoteItemRequest> items) {}

  record StayQuoteItemRequest(UUID roomTypeId, int quantity, int adults, int children) {}

  record StayQuoteSnapshot(
      UUID hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      int days,
      int nights,
      LocalTime standardCheckInTime,
      LocalTime standardCheckOutTime,
      LocalTime expectedCheckInTime,
      LocalTime expectedCheckOutTime,
      String currency,
      long roomSubtotalAmount,
      long earlyCheckInSurchargeAmount,
      long lateCheckOutSurchargeAmount,
      long extraGuestSurchargeAmount,
      long surchargeAmount,
      long subtotalAmount,
      long discountAmount,
      long totalAmount,
      List<StayQuoteItemSnapshot> items) {}

  record StayQuoteItemSnapshot(
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
      List<NightlyPriceSnapshot> nightlyPrices) {}

  record NightlyPriceSnapshot(LocalDate stayDate, long unitPrice) {}
}
