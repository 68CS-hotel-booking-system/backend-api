package com.team.hotelbooking.pricing.domain.service;

import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.StayQuote.NightlyPrice;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PR-02, dùng chung cho báo giá một khách sạn (CalculateStayQuoteService) và tra giá theo lô cho
 * tìm kiếm (ListNightlyRatesService) để hai nơi không tự tính giá khác nhau:
 *
 * <ul>
 *   <li>Giá một đêm = giá riêng trong room_rates nếu có, nếu không thì base_price của loại phòng.
 *   <li>Đêm có giá ≤ 0 là chưa có giá bán hợp lệ; loại phòng đó không bán được cho kỳ lưu trú.
 * </ul>
 */
public final class NightlyPriceResolver {

  public static final int MAX_STAY_NIGHTS = 30;

  private NightlyPriceResolver() {}

  public static void validateStay(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
    if (checkIn == null || checkOut == null) {
      throw PricingException.invalidDateRange("Ngày nhận và trả phòng không được để trống.");
    }

    if (checkIn.isBefore(today)) {
      throw PricingException.invalidDateRange("Ngày nhận phòng không được ở trong quá khứ.");
    }

    if (!checkOut.isAfter(checkIn)) {
      throw PricingException.invalidDateRange(
          "Ngày trả phòng phải sau ngày nhận phòng ít nhất 1 đêm.");
    }

    long stayNights = ChronoUnit.DAYS.between(checkIn, checkOut);
    if (stayNights > MAX_STAY_NIGHTS) {
      throw PricingException.maxStayExceeded("Thời gian lưu trú tối đa là 30 đêm.");
    }
  }

  // Dừng ở đêm đầu tiên chưa có giá hợp lệ; khi đó danh sách giá không đủ kỳ lưu trú.
  public static Resolution resolve(
      long basePrice, Map<LocalDate, Long> dailyAmounts, LocalDate checkIn, LocalDate checkOut) {
    List<NightlyPrice> nightlyPrices = new ArrayList<>();
    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      Long customRate = dailyAmounts.get(date);
      long unitPrice = customRate != null ? customRate : basePrice;
      if (unitPrice <= 0L) {
        return new Resolution(List.copyOf(nightlyPrices), date);
      }
      nightlyPrices.add(new NightlyPrice(date, unitPrice));
    }
    return new Resolution(List.copyOf(nightlyPrices), null);
  }

  public record Resolution(List<NightlyPrice> nightlyPrices, LocalDate firstUnpricedNight) {

    public boolean sellable() {
      return firstUnpricedNight == null;
    }

    public long totalAmount() {
      return nightlyPrices.stream().mapToLong(NightlyPrice::unitPrice).sum();
    }
  }
}
