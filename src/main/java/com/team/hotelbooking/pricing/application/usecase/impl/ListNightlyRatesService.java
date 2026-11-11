package com.team.hotelbooking.pricing.application.usecase.impl;

import com.team.hotelbooking.pricing.application.command.ListNightlyRatesQuery;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.RoomTypePricingSnapshot;
import com.team.hotelbooking.pricing.application.result.RoomTypeNightlyRatesResult;
import com.team.hotelbooking.pricing.application.usecase.ListNightlyRatesUseCase;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import com.team.hotelbooking.pricing.domain.service.NightlyPriceResolver;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tra giá từng đêm theo lô cho tìm kiếm: cùng kiểm ngày và cùng quy tắc giá với báo giá
 * (NightlyPriceResolver), nhưng không ném lỗi khi thiếu giá mà trả sellable = false để bên tìm kiếm
 * loại loại phòng đó. Hai truy vấn cho cả lô: loại phòng (qua hotel) và room_rates.
 */
public final class ListNightlyRatesService implements ListNightlyRatesUseCase {

  private final PricingHotelPort pricingHotelPort;
  private final RoomRateRepository roomRateRepository;
  private final Clock clock;

  public ListNightlyRatesService(
      PricingHotelPort pricingHotelPort, RoomRateRepository roomRateRepository, Clock clock) {
    this.pricingHotelPort = pricingHotelPort;
    this.roomRateRepository = roomRateRepository;
    this.clock = clock;
  }

  @Override
  public List<RoomTypeNightlyRatesResult> execute(ListNightlyRatesQuery query) {
    NightlyPriceResolver.validateStay(query.checkIn(), query.checkOut(), BusinessTime.today(clock));
    if (query.roomTypeIds().isEmpty()) {
      return List.of();
    }

    // Loại phòng không đang bán hoặc thuộc khách sạn không ACTIVE bị bỏ khỏi kết quả.
    List<RoomTypePricingSnapshot> roomTypes =
        pricingHotelPort.findActiveRoomTypes(query.roomTypeIds());
    LocalDate lastStayNight = query.checkOut().minusDays(1);
    Map<UUID, Map<LocalDate, Long>> dailyAmounts =
        roomRateRepository.findDailyAmounts(
            roomTypes.stream().map(RoomTypePricingSnapshot::id).toList(),
            query.checkIn(),
            lastStayNight);

    return roomTypes.stream()
        .sorted(Comparator.comparing(RoomTypePricingSnapshot::id))
        .map(
            roomType ->
                RoomTypeNightlyRatesResult.from(
                    roomType.id(),
                    roomType.hotelId(),
                    NightlyPriceResolver.resolve(
                        roomType.basePrice(),
                        dailyAmounts.getOrDefault(roomType.id(), Map.of()),
                        query.checkIn(),
                        query.checkOut())))
        .toList();
  }
}
