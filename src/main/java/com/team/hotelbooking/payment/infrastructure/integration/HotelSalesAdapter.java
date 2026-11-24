package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelSalesPolicy;
import com.team.hotelbooking.payment.ContractDirectory;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelSalesAdapter implements HotelSalesPolicy {
  private final ContractDirectory contracts;
  private final Clock clock;

  @Override
  public java.util.Set<UUID> effectiveHotelIds() {
    return contracts.findEffectiveHotelIds(clock.instant());
  }

  @Override
  public boolean hasEffectiveContract(UUID hotelId) {
    return contracts.findEffective(hotelId, clock.instant()).isPresent();
  }
}
