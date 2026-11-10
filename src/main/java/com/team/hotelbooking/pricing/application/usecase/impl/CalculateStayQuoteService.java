package com.team.hotelbooking.pricing.application.usecase.impl;

import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery.QuoteItemInput;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelPricingSnapshot;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.RoomTypePricingSnapshot;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.application.usecase.CalculateStayQuoteUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.StayQuote;
import com.team.hotelbooking.pricing.domain.model.StayQuote.ItemQuote;
import com.team.hotelbooking.pricing.domain.model.StayQuote.NightlyPrice;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CalculateStayQuoteService implements CalculateStayQuoteUseCase {

  private static final int MAX_STAY_NIGHTS = 30;
  private static final long EXTRA_GUEST_SURCHARGE_PERCENT = 10L;

  private final PricingHotelPort pricingHotelPort;
  private final RoomRateRepository roomRateRepository;
  private final Clock clock;

  public CalculateStayQuoteService(
      PricingHotelPort pricingHotelPort, RoomRateRepository roomRateRepository, Clock clock) {
    this.pricingHotelPort = pricingHotelPort;
    this.roomRateRepository = roomRateRepository;
    this.clock = clock;
  }

  @Override
  public StayQuoteResult execute(CalculateStayQuoteQuery query) {
    if (query == null || query.hotelId() == null) {
      throw PricingException.invalidQuoteRequest("Thông tin yêu cầu báo giá không được để trống.");
    }

    validateDates(query.checkIn(), query.checkOut());

    if (query.items() == null || query.items().isEmpty()) {
      throw PricingException.invalidQuoteRequest("Danh sách hạng mục phòng không được để trống.");
    }

    HotelPricingSnapshot hotel =
        pricingHotelPort
            .findActiveHotel(query.hotelId())
            .orElseThrow(() -> PricingException.hotelNotFound("Không tìm thấy khách sạn."));

    int nights = (int) ChronoUnit.DAYS.between(query.checkIn(), query.checkOut());
    int days = nights + 1;

    LocalTime standardCheckIn =
        hotel.checkInTime() != null ? hotel.checkInTime() : LocalTime.of(14, 0);
    LocalTime standardCheckOut =
        hotel.checkOutTime() != null ? hotel.checkOutTime() : LocalTime.of(12, 0);
    LocalTime expectedCheckIn =
        query.expectedCheckInTime() != null ? query.expectedCheckInTime() : standardCheckIn;
    LocalTime expectedCheckOut =
        query.expectedCheckOutTime() != null ? query.expectedCheckOutTime() : standardCheckOut;

    int earlyCheckInPercent = resolveEarlyCheckInPercent(expectedCheckIn, standardCheckIn);
    int lateCheckOutPercent = resolveLateCheckOutPercent(expectedCheckOut, standardCheckOut);

    LocalDate lastStayNight = query.checkOut().minusDays(1);

    List<ItemQuote> itemQuotes = new ArrayList<>();
    long totalRoomSubtotal = 0L;
    long totalEarlyCheckInSurcharge = 0L;
    long totalLateCheckOutSurcharge = 0L;
    long totalExtraGuestSurcharge = 0L;

    for (QuoteItemInput item : query.items()) {
      if (item == null || item.roomTypeId() == null) {
        throw PricingException.invalidQuoteRequest("Mã loại phòng không được để trống.");
      }

      RoomTypePricingSnapshot roomType =
          pricingHotelPort
              .findActiveRoomType(item.roomTypeId(), query.hotelId())
              .orElseThrow(
                  () ->
                      PricingException.roomTypeNotFound(
                          "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

      validateItemOccupancy(item, roomType);

      Map<LocalDate, Long> dailyAmounts =
          roomRateRepository.findDailyAmounts(roomType.id(), query.checkIn(), lastStayNight);

      List<NightlyPrice> nightlyPrices = new ArrayList<>(nights);
      long singleRoomNightsSum = 0L;
      for (LocalDate date = query.checkIn();
          date.isBefore(query.checkOut());
          date = date.plusDays(1)) {
        Long customRate = dailyAmounts.get(date);
        long unitPrice = customRate != null ? customRate : roomType.basePrice();
        if (unitPrice <= 0L) {
          throw PricingException.rateNotAvailable(
              "Loại phòng chưa được thiết lập giá bán hợp lệ cho đêm " + date + ".");
        }
        nightlyPrices.add(new NightlyPrice(date, unitPrice));
        singleRoomNightsSum += unitPrice;
      }

      long itemRoomSubtotal = item.quantity() * singleRoomNightsSum;
      long firstNightRoomAmount = item.quantity() * nightlyPrices.get(0).unitPrice();
      long lastNightRoomAmount =
          item.quantity() * nightlyPrices.get(nightlyPrices.size() - 1).unitPrice();

      long itemEarlySurcharge = (firstNightRoomAmount * earlyCheckInPercent) / 100L;
      long itemLateSurcharge = (lastNightRoomAmount * lateCheckOutPercent) / 100L;

      int standardAdultCapacity = item.quantity() * roomType.maxAdults();
      int totalGuests = item.adults() + item.children();
      int extraGuests = Math.max(0, totalGuests - standardAdultCapacity);
      long itemExtraGuestSurcharge =
          (extraGuests * singleRoomNightsSum * EXTRA_GUEST_SURCHARGE_PERCENT) / 100L;

      long itemSurcharge = itemEarlySurcharge + itemLateSurcharge + itemExtraGuestSurcharge;
      long itemSubtotal = itemRoomSubtotal + itemSurcharge;

      itemQuotes.add(
          new ItemQuote(
              roomType.id(),
              roomType.name(),
              item.quantity(),
              item.adults(),
              item.children(),
              extraGuests,
              itemRoomSubtotal,
              itemEarlySurcharge,
              itemLateSurcharge,
              itemExtraGuestSurcharge,
              itemSurcharge,
              itemSubtotal,
              List.copyOf(nightlyPrices)));

      totalRoomSubtotal += itemRoomSubtotal;
      totalEarlyCheckInSurcharge += itemEarlySurcharge;
      totalLateCheckOutSurcharge += itemLateSurcharge;
      totalExtraGuestSurcharge += itemExtraGuestSurcharge;
    }

    long totalSurcharge =
        totalEarlyCheckInSurcharge + totalLateCheckOutSurcharge + totalExtraGuestSurcharge;
    long subtotalAmount = totalRoomSubtotal + totalSurcharge;
    long discountAmount = 0L;
    long totalAmount = subtotalAmount - discountAmount;

    StayQuote quote =
        new StayQuote(
            hotel.id(),
            query.checkIn(),
            query.checkOut(),
            days,
            nights,
            standardCheckIn,
            standardCheckOut,
            expectedCheckIn,
            expectedCheckOut,
            "VND",
            totalRoomSubtotal,
            totalEarlyCheckInSurcharge,
            totalLateCheckOutSurcharge,
            totalExtraGuestSurcharge,
            totalSurcharge,
            subtotalAmount,
            discountAmount,
            totalAmount,
            List.copyOf(itemQuotes));

    return StayQuoteResult.from(quote);
  }

  private void validateDates(LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null) {
      throw PricingException.invalidDateRange("Ngày nhận và trả phòng không được để trống.");
    }

    LocalDate today = LocalDate.now(clock);
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

  private static void validateItemOccupancy(QuoteItemInput item, RoomTypePricingSnapshot roomType) {
    if (item.quantity() < 1) {
      throw PricingException.invalidOccupancy("Số lượng phòng phải lớn hơn hoặc bằng 1.");
    }
    if (item.adults() < item.quantity()) {
      throw PricingException.invalidOccupancy("Mỗi phòng phải có ít nhất 1 người lớn.");
    }
    if (item.children() < 0) {
      throw PricingException.invalidOccupancy("Số lượng trẻ em không được âm.");
    }
    int maxAdultsAllowed = item.quantity() * roomType.maxAdults();
    int maxChildrenAllowed = item.quantity() * roomType.maxChildren();
    int maxTotalAllowed =
        item.quantity()
            * Math.max(roomType.maxOccupancy(), roomType.maxAdults() + roomType.maxChildren());

    if (item.adults() > maxAdultsAllowed
        || item.children() > maxChildrenAllowed
        || (item.adults() + item.children()) > maxTotalAllowed) {
      throw PricingException.invalidOccupancy(
          "Số lượng khách vượt quá sức chứa tối đa của loại phòng.");
    }
  }

  private static int resolveEarlyCheckInPercent(
      LocalTime expectedCheckIn, LocalTime standardCheckIn) {
    if (!expectedCheckIn.isBefore(standardCheckIn)) {
      return 0;
    }
    if (expectedCheckIn.isBefore(LocalTime.of(5, 0))) {
      return 100;
    }
    if (expectedCheckIn.isBefore(LocalTime.of(9, 0))) {
      return 50;
    }
    return 30;
  }

  private static int resolveLateCheckOutPercent(
      LocalTime expectedCheckOut, LocalTime standardCheckOut) {
    if (!expectedCheckOut.isAfter(standardCheckOut)) {
      return 0;
    }
    if (!expectedCheckOut.isAfter(LocalTime.of(15, 0))) {
      return 30;
    }
    if (!expectedCheckOut.isAfter(LocalTime.of(18, 0))) {
      return 50;
    }
    return 100;
  }
}
