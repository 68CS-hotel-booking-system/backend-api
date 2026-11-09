package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelCatalogAdapter implements HotelCatalogPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public Optional<HotelSummary> findHotelSummary(UUID hotelId) {
    return hotelDirectory
        .findHotelSummary(hotelId)
        .map(h -> new HotelSummary(h.id(), h.active(), h.verified()));
  }

  @Override
  public List<RoomTypeSummary> findActiveRoomTypes(UUID hotelId) {
    return hotelDirectory.findActiveRoomTypes(hotelId).stream()
        .map(
            rt ->
                new RoomTypeSummary(
                    rt.id(),
                    rt.name(),
                    rt.maxOccupancy(),
                    rt.maxAdults(),
                    rt.maxChildren(),
                    rt.basePrice()))
        .toList();
  }
}
