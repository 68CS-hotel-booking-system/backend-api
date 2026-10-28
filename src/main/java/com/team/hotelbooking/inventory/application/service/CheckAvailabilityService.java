package com.team.hotelbooking.inventory.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import com.team.hotelbooking.inventory.domain.model.RoomTypeAvailability;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckAvailabilityService implements CheckAvailabilityUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;
  private final InventoryQueryPort inventoryQueryPort;
  private final RoomPricingPort roomPricingPort;

  @Override
  public HotelAvailability execute(CheckAvailabilityQuery query) {
    validateDates(query.checkInDate(), query.checkOutDate());

    HotelJpaEntity hotel =
        hotelJpaRepository
            .findByIdAndActiveTrue(query.hotelId())
            .orElseThrow(
                () ->
                    InventoryException.hotelNotFound("Khách sạn không tồn tại hoặc chưa mở bán."));

    if (hotel.getStatus() != HotelJpaEntity.DbHotelStatus.active) {
      throw InventoryException.hotelNotFound("Khách sạn hiện đang tạm ngừng kinh doanh.");
    }

    int totalNights = (int) ChronoUnit.DAYS.between(query.checkInDate(), query.checkOutDate());
    int requestedQuantity =
        (query.quantity() != null && query.quantity() > 0) ? query.quantity() : 1;
    int requestedAdults = (query.adults() != null && query.adults() > 0) ? query.adults() : 1;
    int requestedChildren =
        (query.children() != null && query.children() >= 0) ? query.children() : 0;
    int totalGuests = requestedAdults + requestedChildren;

    List<RoomTypeJpaEntity> roomTypes =
        roomTypeJpaRepository.findAllByHotelIdAndActiveTrue(query.hotelId());

    List<RoomTypeAvailability> availabilityList = new ArrayList<>();

    for (RoomTypeJpaEntity rt : roomTypes) {
      if (rt.getMaxOccupancy() < totalGuests || rt.getMaxAdults() < requestedAdults) {
        continue;
      }

      int availableQty =
          inventoryQueryPort.calculateAvailableQuantity(
              rt.getId(), query.checkInDate(), query.checkOutDate());

      boolean isAvailable = availableQty >= requestedQuantity;

      // Tính toán giá cho kỳ nghỉ
      LocalDate lastNight = query.checkOutDate().minusDays(1);
      Map<LocalDate, Long> rateMap =
          roomPricingPort.getDailyRates(rt.getId(), query.checkInDate(), lastNight);

      long totalEstimatedPrice = 0L;
      for (LocalDate date = query.checkInDate();
          date.isBefore(query.checkOutDate());
          date = date.plusDays(1)) {
        Long nightlyAmount = rateMap.get(date);
        if (nightlyAmount != null) {
          totalEstimatedPrice += nightlyAmount;
        } else {
          totalEstimatedPrice += (rt.getBasePrice() != null ? rt.getBasePrice() : 0L);
        }
      }

      long pricePerNight =
          totalNights > 0 ? (totalEstimatedPrice / totalNights) : totalEstimatedPrice;

      availabilityList.add(
          new RoomTypeAvailability(
              rt.getId(),
              rt.getName(),
              rt.getMaxOccupancy().intValue(),
              rt.getMaxAdults().intValue(),
              rt.getMaxChildren().intValue(),
              availableQty,
              isAvailable,
              pricePerNight,
              totalEstimatedPrice,
              "VND"));
    }

    return new HotelAvailability(
        hotel.getId(), query.checkInDate(), query.checkOutDate(), totalNights, availabilityList);
  }

  private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
    if (checkInDate == null || checkOutDate == null) {
      throw InventoryException.invalidDateRange("Ngày nhận và trả phòng không được để trống.");
    }

    if (checkInDate.isBefore(LocalDate.now())) {
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
