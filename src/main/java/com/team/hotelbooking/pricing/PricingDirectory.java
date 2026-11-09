package com.team.hotelbooking.pricing;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface PricingDirectory {

  /** Trả giá VND theo ngày, bao gồm cả startDate và endDate; bỏ qua ngày chưa có giá riêng. */
  Map<LocalDate, Long> getDailyRates(UUID roomTypeId, LocalDate startDate, LocalDate endDate);
}
