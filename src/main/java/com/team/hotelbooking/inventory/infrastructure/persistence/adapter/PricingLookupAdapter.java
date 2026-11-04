package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import com.team.hotelbooking.pricing.infrastructure.persistence.repository.RoomRateJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingLookupAdapter implements RoomPricingPort {

  private final RoomRateJpaRepository roomRateJpaRepository;

  @Override
  public Map<LocalDate, Long> getDailyRates(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    List<RoomRateJpaEntity> rates =
        roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, startDate, endDate);

    return rates.stream()
        .collect(
            Collectors.toMap(
                r -> r.getId().getRateDate(),
                RoomRateJpaEntity::getAmount,
                (first, second) -> first));
  }
}
