package com.team.hotelbooking.inventory.application.port;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface RoomPricingPort {

  /** Lấy bản đồ giá phòng theo từng ngày lưu trú (VND). */
  Map<LocalDate, Long> getDailyRates(UUID roomTypeId, LocalDate startDate, LocalDate endDate);
}
