package com.team.hotelbooking.pricing.application.service;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.SetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.DailyRate;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateIdEmbeddable;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import com.team.hotelbooking.pricing.infrastructure.persistence.repository.RoomRateJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SetRoomRatesService implements SetRoomRatesUseCase {

  private final PricingHotelPort pricingHotelPort;
  private final RoomRateJpaRepository roomRateJpaRepository;
  private final Clock clock;

  @Override
  public RoomTypeRatesResult execute(SetRoomRatesCommand command) {
    if (command.amount() == null || command.amount() <= 0) {
      throw PricingException.invalidAmount("Giá phòng phải lớn hơn 0.");
    }

    LocalDate startDate = command.startDate();
    if (startDate == null) {
      throw PricingException.invalidDateRange("Ngày bắt đầu không được để trống.");
    }

    LocalDate resolvedEndDate;
    if (command.endDateExclusive() == null) {
      resolvedEndDate = startDate;
    } else {
      if (!command.endDateExclusive().isAfter(startDate)) {
        throw PricingException.invalidDateRange("Ngày kết thúc phải sau ngày bắt đầu.");
      }
      resolvedEndDate = command.endDateExclusive().minusDays(1);
    }

    long days = ChronoUnit.DAYS.between(startDate, resolvedEndDate);
    if (days > 365) {
      throw PricingException.maxRangeExceeded("Khoảng thời gian thiết lập giá tối đa là 365 ngày.");
    }

    LocalDate today = LocalDate.now(clock);
    if (startDate.isBefore(today)) {
      throw PricingException.invalidDateRange("Không thể đặt giá cho ngày trong quá khứ.");
    }

    // Kiểm quyền và trạng thái hotel trước, rồi mới xét loại phòng thuộc hotel.
    PricingManagementAccess.require(
        pricingHotelPort, command.hotelId(), command.actorUserId(), command.admin());

    if (!pricingHotelPort.isRoomTypeActiveInHotel(command.roomTypeId(), command.hotelId())) {
      throw PricingException.roomTypeNotFound(
          "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn.");
    }

    List<RoomRateJpaEntity> existingRates =
        roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(
            command.roomTypeId(), startDate, resolvedEndDate);

    Map<LocalDate, RoomRateJpaEntity> rateMap =
        existingRates.stream().collect(Collectors.toMap(r -> r.getId().getRateDate(), r -> r));

    Instant now = clock.instant();
    List<RoomRateJpaEntity> entitiesToSave = new ArrayList<>();
    List<DailyRate> dailyRates = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(resolvedEndDate); date = date.plusDays(1)) {
      RoomRateJpaEntity entity = rateMap.get(date);
      if (entity != null) {
        entity.setAmount(command.amount());
        entity.setCurrency(command.currency());
        entity.setUpdatedAt(now);
        entitiesToSave.add(entity);
      } else {
        RoomRateJpaEntity newEntity = new RoomRateJpaEntity();
        newEntity.setId(new RoomRateIdEmbeddable(command.roomTypeId(), date));
        newEntity.setAmount(command.amount());
        newEntity.setCurrency(command.currency());
        newEntity.setCreatedAt(now);
        newEntity.setUpdatedAt(now);
        entitiesToSave.add(newEntity);
      }
      dailyRates.add(new DailyRate(date, command.amount(), true, now));
    }

    roomRateJpaRepository.saveAll(entitiesToSave);

    RoomTypeRates domain =
        new RoomTypeRates(
            command.hotelId(),
            command.roomTypeId(),
            command.currency(),
            startDate,
            resolvedEndDate,
            dailyRates);

    return RoomTypeRatesResult.from(domain);
  }
}
