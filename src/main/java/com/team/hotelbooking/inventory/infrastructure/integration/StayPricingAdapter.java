package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.inventory.application.port.StayPricingPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.pricing.StayQuoteDirectory;
import com.team.hotelbooking.pricing.StayQuoteDirectory.StayQuoteCalculationException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StayPricingAdapter implements StayPricingPort {

  private final StayQuoteDirectory stayQuoteDirectory;

  @Override
  public Map<UUID, RoomTypeStayPrice> findStayPrices(
      Collection<UUID> roomTypeIds, LocalDate checkIn, LocalDate checkOut) {
    Map<UUID, StayQuoteDirectory.NightlyRatesSnapshot> snapshots;
    try {
      snapshots = stayQuoteDirectory.findNightlyRates(roomTypeIds, checkIn, checkOut);
    } catch (StayQuoteCalculationException ex) {
      // Tìm kiếm đã kiểm ngày trước khi gọi; giữ thông điệp của pricing nếu vẫn lệch quy tắc.
      throw switch (ex.reason()) {
        case MAX_STAY_EXCEEDED -> InventoryException.maxStayExceeded(ex.getMessage());
        default -> InventoryException.invalidDateRange(ex.getMessage());
      };
    }

    Map<UUID, RoomTypeStayPrice> prices = new HashMap<>();
    snapshots.forEach(
        (roomTypeId, snapshot) ->
            prices.put(
                roomTypeId,
                new RoomTypeStayPrice(roomTypeId, snapshot.sellable(), snapshot.totalAmount())));
    return prices;
  }
}
