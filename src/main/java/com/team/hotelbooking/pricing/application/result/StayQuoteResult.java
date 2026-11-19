package com.team.hotelbooking.pricing.application.result;

import com.team.hotelbooking.pricing.domain.model.StayQuote;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record StayQuoteResult(
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
    List<ItemQuoteResult> items) {

  public record ItemQuoteResult(
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

    public static ItemQuoteResult from(StayQuote.ItemQuote item) {
      return new ItemQuoteResult(
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
          item.nightlyPrices().stream().map(NightlyPriceResult::from).toList());
    }
  }

  public record NightlyPriceResult(LocalDate stayDate, long unitPrice) {
    public static NightlyPriceResult from(StayQuote.NightlyPrice np) {
      return new NightlyPriceResult(np.stayDate(), np.unitPrice());
    }
  }

  public static StayQuoteResult from(StayQuote domain) {
    return new StayQuoteResult(
        domain.hotelId(),
        domain.checkIn(),
        domain.checkOut(),
        domain.days(),
        domain.nights(),
        domain.standardCheckInTime(),
        domain.standardCheckOutTime(),
        domain.expectedCheckInTime(),
        domain.expectedCheckOutTime(),
        domain.currency(),
        domain.roomSubtotalAmount(),
        domain.earlyCheckInSurchargeAmount(),
        domain.lateCheckOutSurchargeAmount(),
        domain.extraGuestSurchargeAmount(),
        domain.surchargeAmount(),
        domain.subtotalAmount(),
        domain.discountAmount(),
        domain.totalAmount(),
        domain.items().stream().map(ItemQuoteResult::from).toList());
  }
}
