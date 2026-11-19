package com.team.hotelbooking.booking.application.port;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface BookingPricingPort {

  CalculatedStayQuote calculateQuote(StayQuoteCriteria criteria);

  record StayQuoteCriteria(
      UUID hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      LocalTime expectedCheckInTime,
      LocalTime expectedCheckOutTime,
      List<StayQuoteItemCriteria> items) {}

  record StayQuoteItemCriteria(UUID roomTypeId, int quantity, int adults, int children) {}

  record CalculatedStayQuote(
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
      List<CalculatedItemQuote> items) {}

  record CalculatedItemQuote(
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
      List<CalculatedNightlyPrice> nightlyPrices) {}

  record CalculatedNightlyPrice(LocalDate stayDate, long unitPrice) {}
}
