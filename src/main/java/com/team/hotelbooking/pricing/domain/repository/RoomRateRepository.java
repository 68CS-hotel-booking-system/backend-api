package com.team.hotelbooking.pricing.domain.repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface RoomRateRepository {

  Map<LocalDate, Long> findDailyAmounts(UUID roomTypeId, LocalDate startDate, LocalDate endDate);
}
