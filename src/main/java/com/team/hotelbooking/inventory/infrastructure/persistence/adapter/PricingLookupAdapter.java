package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.pricing.PricingDirectory;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingLookupAdapter implements RoomPricingPort {

  private final PricingDirectory pricingDirectory;

  @Override
  public Map<LocalDate, Long> getDailyRates(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    return pricingDirectory.getDailyRates(roomTypeId, startDate, endDate);
  }
}
