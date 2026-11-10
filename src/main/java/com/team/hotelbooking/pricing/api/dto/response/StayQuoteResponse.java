package com.team.hotelbooking.pricing.api.dto.response;

import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record StayQuoteResponse(
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
    List<ItemQuoteResponse> items) {

  public record ItemQuoteResponse(
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

    public static ItemQuoteResponse from(StayQuoteResult.ItemQuoteResult item) {
      return new ItemQuoteResponse(
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
    public static NightlyPriceResponse from(StayQuoteResult.NightlyPriceResult np) {
      return new NightlyPriceResponse(np.stayDate(), np.unitPrice());
    }
  }

  public static StayQuoteResponse from(StayQuoteResult result) {
    return new StayQuoteResponse(
        result.hotelId(),
        result.checkIn(),
        result.checkOut(),
        result.days(),
        result.nights(),
        result.standardCheckInTime(),
        result.standardCheckOutTime(),
        result.expectedCheckInTime(),
        result.expectedCheckOutTime(),
        result.currency(),
        result.roomSubtotalAmount(),
        result.earlyCheckInSurchargeAmount(),
        result.lateCheckOutSurchargeAmount(),
        result.extraGuestSurchargeAmount(),
        result.surchargeAmount(),
        result.subtotalAmount(),
        result.discountAmount(),
        result.totalAmount(),
        result.items().stream().map(ItemQuoteResponse::from).toList());
  }
}
