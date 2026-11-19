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
import com.team.hotelbooking.pricing.domain.service.NightlyPriceResolver;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CalculateStayQuoteService implements CalculateStayQuoteUseCase {

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

    LocalDate lastStayNight = query.checkOut().minusDays(1);

    List<ItemQuote> itemQuotes = new ArrayList<>();
    long totalRoomSubtotal = 0L;

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

      // Quy tắc giá từng đêm dùng chung với tra giá theo lô (NightlyPriceResolver).
      NightlyPriceResolver.Resolution resolution =
          NightlyPriceResolver.resolve(
              roomType.basePrice(), dailyAmounts, query.checkIn(), query.checkOut());
      if (!resolution.sellable()) {
        throw PricingException.rateNotAvailable(
            "Loại phòng chưa được thiết lập giá bán hợp lệ cho đêm "
                + resolution.firstUnpricedNight()
                + ".");
      }
      List<NightlyPrice> nightlyPrices = resolution.nightlyPrices();
      long singleRoomNightsSum = resolution.totalAmount();

      long itemRoomSubtotal = item.quantity() * singleRoomNightsSum;

      itemQuotes.add(
          new ItemQuote(
              roomType.id(),
              roomType.name(),
              item.quantity(),
              item.adults(),
              item.children(),
              0,
              itemRoomSubtotal,
              0L,
              0L,
              0L,
              0L,
              itemRoomSubtotal,
              List.copyOf(nightlyPrices)));

      totalRoomSubtotal += itemRoomSubtotal;
    }

    long subtotalAmount = totalRoomSubtotal;
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
            0L,
            0L,
            0L,
            0L,
            subtotalAmount,
            discountAmount,
            totalAmount,
            List.copyOf(itemQuotes));

    return StayQuoteResult.from(quote);
  }

  private void validateDates(LocalDate checkIn, LocalDate checkOut) {
    NightlyPriceResolver.validateStay(checkIn, checkOut, BusinessTime.today(clock));
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
    long maxAdultsAllowed = (long) item.quantity() * roomType.maxAdults();
    long maxChildrenAllowed = (long) item.quantity() * roomType.maxChildren();
    long maxTotalAllowed = (long) item.quantity() * roomType.maxOccupancy();

    if (item.adults() > maxAdultsAllowed
        || item.children() > maxChildrenAllowed
        || ((long) item.adults() + item.children()) > maxTotalAllowed) {
      throw PricingException.invalidOccupancy(
          "Số lượng khách vượt quá sức chứa tối đa của loại phòng.");
    }
  }
}
