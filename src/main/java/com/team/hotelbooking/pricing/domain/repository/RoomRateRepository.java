package com.team.hotelbooking.pricing.domain.repository;

import com.team.hotelbooking.pricing.domain.model.RoomRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RoomRateRepository {

  List<RoomRate> findRatesByRoomTypeIdAndDateRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate);

  List<RoomRate> saveAll(List<RoomRate> rates);

  Map<LocalDate, Long> findDailyAmounts(UUID roomTypeId, LocalDate startDate, LocalDate endDate);
}
