package com.team.hotelbooking.inventory.application.service;

import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import com.team.hotelbooking.inventory.domain.model.RoomTypeAvailability;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CheckAvailabilityService implements CheckAvailabilityUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final InventoryQueryPort inventoryQueryPort;
  private final RoomPricingPort roomPricingPort;
  private final Clock clock;

  @Override
  public CheckAvailabilityResult execute(CheckAvailabilityQuery query) {
    validateDates(query.checkInDate(), query.checkOutDate());

    HotelCatalogPort.HotelSummary hotel =
        hotelCatalogPort
            .findHotelSummary(query.hotelId())
            .orElseThrow(
                () ->
                    InventoryException.hotelNotFound("Khách sạn không tồn tại hoặc chưa mở bán."));

    if (hotel.isSuspended()) {
      throw InventoryException.hotelNotFound("Khách sạn hiện đang tạm ngừng kinh doanh.");
    }

    int totalNights = (int) ChronoUnit.DAYS.between(query.checkInDate(), query.checkOutDate());
    int requestedQuantity =
        (query.quantity() != null && query.quantity() > 0) ? query.quantity() : 1;
    int requestedAdults = (query.adults() != null && query.adults() > 0) ? query.adults() : 1;
    int requestedChildren =
        (query.children() != null && query.children() >= 0) ? query.children() : 0;
    int totalGuests = requestedAdults + requestedChildren;

    List<HotelCatalogPort.RoomTypeSummary> roomTypes =
        hotelCatalogPort.findActiveRoomTypes(query.hotelId());

    List<RoomTypeAvailability> availabilityList = new ArrayList<>();

    for (HotelCatalogPort.RoomTypeSummary rt : roomTypes) {
      if (rt.maxOccupancy() < totalGuests || rt.maxAdults() < requestedAdults) {
        continue;
      }

      int availableQty =
          inventoryQueryPort.calculateAvailableQuantity(
              rt.id(), query.checkInDate(), query.checkOutDate());

      // Tính toán giá cho kỳ nghỉ (PR-01, PR-02, HT-03: mọi đêm phải có giá hợp lệ > 0)
      LocalDate lastNight = query.checkOutDate().minusDays(1);
      Map<LocalDate, Long> rateMap =
          roomPricingPort.getDailyRates(rt.id(), query.checkInDate(), lastNight);

      long totalEstimatedPrice = 0L;
      boolean hasValidRateForAllNights = true;
      for (LocalDate date = query.checkInDate();
          date.isBefore(query.checkOutDate());
          date = date.plusDays(1)) {
        Long nightlyAmount =
            rateMap != null && rateMap.containsKey(date) ? rateMap.get(date) : rt.basePrice();
        if (nightlyAmount == null || nightlyAmount <= 0L) {
          hasValidRateForAllNights = false;
          break;
        }
        totalEstimatedPrice += nightlyAmount;
      }

      int effectiveAvailableQty = hasValidRateForAllNights ? availableQty : 0;
      boolean isAvailable = hasValidRateForAllNights && availableQty >= requestedQuantity;
      Long pricePerNight =
          hasValidRateForAllNights
              ? (totalNights > 0 ? (totalEstimatedPrice / totalNights) : totalEstimatedPrice)
              : null;
      Long estimatedTotal = hasValidRateForAllNights ? totalEstimatedPrice : null;

      availabilityList.add(
          new RoomTypeAvailability(
              rt.id(),
              rt.name(),
              rt.maxOccupancy(),
              rt.maxAdults(),
              rt.maxChildren(),
              effectiveAvailableQty,
              isAvailable,
              pricePerNight,
              estimatedTotal,
              "VND"));
    }

    HotelAvailability domain =
        new HotelAvailability(
            hotel.id(), query.checkInDate(), query.checkOutDate(), totalNights, availabilityList);

    return CheckAvailabilityResult.from(domain);
  }

  private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
    if (checkInDate == null || checkOutDate == null) {
      throw InventoryException.invalidDateRange("Ngày nhận và trả phòng không được để trống.");
    }

    LocalDate today = BusinessTime.today(clock);
    if (checkInDate.isBefore(today)) {
      throw InventoryException.invalidDateRange("Ngày nhận phòng không thể ở trong quá khứ.");
    }

    if (!checkOutDate.isAfter(checkInDate)) {
      throw InventoryException.invalidDateRange(
          "Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm.");
    }

    long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    if (stayDays > 30) {
      throw InventoryException.maxStayExceeded("Thời gian lưu trú tối đa là 30 đêm.");
    }
  }
}
