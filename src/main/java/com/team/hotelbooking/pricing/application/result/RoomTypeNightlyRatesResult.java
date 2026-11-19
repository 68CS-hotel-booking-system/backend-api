package com.team.hotelbooking.pricing.application.result;

import com.team.hotelbooking.pricing.domain.model.StayQuote.NightlyPrice;
import com.team.hotelbooking.pricing.domain.service.NightlyPriceResolver;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// sellable = mọi đêm đều có giá hợp lệ; khi false, nightlyPrices chỉ gồm các đêm trước đêm thiếu
// giá.
public record RoomTypeNightlyRatesResult(
    UUID roomTypeId,
    UUID hotelId,
    boolean sellable,
    long totalAmount,
    List<NightlyPriceResult> nightlyPrices) {

  public RoomTypeNightlyRatesResult {
    nightlyPrices = List.copyOf(nightlyPrices);
  }

  public static RoomTypeNightlyRatesResult from(
      UUID roomTypeId, UUID hotelId, NightlyPriceResolver.Resolution resolution) {
    return new RoomTypeNightlyRatesResult(
        roomTypeId,
        hotelId,
        resolution.sellable(),
        resolution.totalAmount(),
        resolution.nightlyPrices().stream().map(NightlyPriceResult::from).toList());
  }

  public record NightlyPriceResult(LocalDate stayDate, long unitPrice) {

    public static NightlyPriceResult from(NightlyPrice price) {
      return new NightlyPriceResult(price.stayDate(), price.unitPrice());
    }
  }
}
