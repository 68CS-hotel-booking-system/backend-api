package com.team.hotelbooking.pricing.application.service;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.SetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.DailyRate;
import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
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
  private final RoomRateRepository roomRateRepository;
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

    LocalDate today = BusinessTime.today(clock);
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

    List<RoomRate> existingRates =
        roomRateRepository.findRatesByRoomTypeIdAndDateRange(
            command.roomTypeId(), startDate, resolvedEndDate);

    Map<LocalDate, RoomRate> rateMap =
        existingRates.stream()
            .collect(Collectors.toMap(RoomRate::rateDate, r -> r, (first, second) -> first));

    Instant now = clock.instant();
    String currency =
        command.currency() == null || command.currency().isBlank()
            ? "VND"
            : command.currency().trim().toUpperCase();
    List<RoomRate> ratesToSave = new ArrayList<>();
    List<DailyRate> dailyRates = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(resolvedEndDate); date = date.plusDays(1)) {
      RoomRate existing = rateMap.get(date);
      Instant createdAt =
          existing != null && existing.createdAt() != null ? existing.createdAt() : now;
      ratesToSave.add(
          new RoomRate(
              command.roomTypeId(),
              date,
              command.amount(),
              currency,
              command.actorUserId(),
              createdAt,
              now));
      dailyRates.add(new DailyRate(date, command.amount(), true, now));
    }

    roomRateRepository.saveAll(ratesToSave);

    RoomTypeRates domain =
        new RoomTypeRates(
            command.hotelId(),
            command.roomTypeId(),
            currency,
            startDate,
            resolvedEndDate,
            dailyRates);

    return RoomTypeRatesResult.from(domain);
  }
}
