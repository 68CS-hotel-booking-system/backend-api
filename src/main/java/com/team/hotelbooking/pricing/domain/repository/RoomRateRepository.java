package com.team.hotelbooking.pricing.domain.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface RoomRateRepository {

  Map<LocalDate, Long> findDailyAmounts(UUID roomTypeId, LocalDate startDate, LocalDate endDate);

  // Theo lô: một truy vấn cho nhiều loại phòng; loại phòng không có giá riêng không có trong map.
  Map<UUID, Map<LocalDate, Long>> findDailyAmounts(
      Collection<UUID> roomTypeIds, LocalDate startDate, LocalDate endDate);
}
