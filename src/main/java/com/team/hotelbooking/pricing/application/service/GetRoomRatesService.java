package com.team.hotelbooking.pricing.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.DailyRate;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import com.team.hotelbooking.pricing.infrastructure.persistence.repository.RoomRateJpaRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRoomRatesService implements GetRoomRatesUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;
  private final RoomRateJpaRepository roomRateJpaRepository;

  @Override
  public RoomTypeRates execute(GetRoomRatesQuery query) {
    validateDates(query.startDate(), query.endDate());

    hotelJpaRepository
        .findByIdAndActiveTrue(query.hotelId())
        .orElseThrow(() -> PricingException.hotelNotFound("Không tìm thấy khách sạn."));

    roomTypeJpaRepository
        .findByIdAndHotelIdAndActiveTrue(query.roomTypeId(), query.hotelId())
        .orElseThrow(
            () ->
                PricingException.roomTypeNotFound(
                    "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

    List<RoomRateJpaEntity> ratesFromDb =
        roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(
            query.roomTypeId(), query.startDate(), query.endDate());

    Map<LocalDate, RoomRateJpaEntity> rateMap =
        ratesFromDb.stream().collect(Collectors.toMap(r -> r.getId().getRateDate(), r -> r));

    List<DailyRate> dailyRates = new ArrayList<>();
    for (LocalDate date = query.startDate();
        !date.isAfter(query.endDate());
        date = date.plusDays(1)) {
      RoomRateJpaEntity entity = rateMap.get(date);
      if (entity != null) {
        dailyRates.add(new DailyRate(date, entity.getAmount(), true, entity.getUpdatedAt()));
      } else {
        dailyRates.add(new DailyRate(date, null, false, null));
      }
    }

    return new RoomTypeRates(
        query.hotelId(), query.roomTypeId(), "VND", query.startDate(), query.endDate(), dailyRates);
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
