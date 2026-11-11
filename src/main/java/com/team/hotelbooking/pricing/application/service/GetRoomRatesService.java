package com.team.hotelbooking.pricing.application.service;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.DailyRate;
import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetRoomRatesService implements GetRoomRatesUseCase {

  private final PricingHotelPort pricingHotelPort;
  private final RoomRateRepository roomRateRepository;

  @Override
  public RoomTypeRatesResult execute(GetRoomRatesQuery query) {
    validateDates(query.startDate(), query.endDate());

    // Kiểm quyền và trạng thái hotel trước, rồi mới xét loại phòng thuộc hotel.
    PricingManagementAccess.require(
        pricingHotelPort, query.hotelId(), query.actorUserId(), query.admin());

    if (!pricingHotelPort.isRoomTypeActiveInHotel(query.roomTypeId(), query.hotelId())) {
      throw PricingException.roomTypeNotFound(
          "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn.");
    }

    List<RoomRate> ratesFromDb =
        roomRateRepository.findRatesByRoomTypeIdAndDateRange(
            query.roomTypeId(), query.startDate(), query.endDate());

    Map<LocalDate, RoomRate> rateMap =
        ratesFromDb.stream()
            .collect(Collectors.toMap(RoomRate::rateDate, r -> r, (first, second) -> first));

    List<DailyRate> dailyRates = new ArrayList<>();
    for (LocalDate date = query.startDate();
        !date.isAfter(query.endDate());
        date = date.plusDays(1)) {
      RoomRate rate = rateMap.get(date);
      if (rate != null && rate.amount() != null && rate.amount() > 0) {
        dailyRates.add(new DailyRate(date, rate.amount(), true, rate.updatedAt()));
      } else {
        dailyRates.add(new DailyRate(date, null, false, null));
      }
    }

    RoomTypeRates domain =
        new RoomTypeRates(
            query.hotelId(),
            query.roomTypeId(),
            "VND",
            query.startDate(),
            query.endDate(),
            dailyRates);

    return RoomTypeRatesResult.from(domain);
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw PricingException.invalidDateRange("Ngày bắt đầu và kết thúc không được để trống.");
    }

    if (endDate.isBefore(startDate)) {
      throw PricingException.invalidDateRange("Ngày kết thúc không được trước ngày bắt đầu.");
    }

    long days = ChronoUnit.DAYS.between(startDate, endDate);
    if (days > 90) {
      throw PricingException.maxRangeExceeded("Khoảng thời gian tra cứu giá tối đa là 90 ngày.");
    }
  }
}
