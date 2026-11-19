package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.hotel.HotelDirectory.HotelSearchFilter;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelSearchCatalogAdapter implements HotelSearchCatalogPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public List<SearchableHotel> findSearchableHotels(
      UUID cityId, String citySlug, Integer minStarRating, Set<UUID> amenityIds) {
    return hotelDirectory
        .findActiveHotelsForSearch(
            new HotelSearchFilter(cityId, citySlug, minStarRating, amenityIds))
        .stream()
        .map(
            h ->
                new SearchableHotel(
                    h.id(),
                    h.name(),
                    h.slug(),
                    h.address(),
                    h.starRating(),
                    new City(h.city().id(), h.city().name(), h.city().slug()),
                    h.amenities().stream()
                        .map(a -> new Amenity(a.id(), a.code(), a.name()))
                        .toList()))
        .toList();
  }

  @Override
  public List<CatalogRoomType> findActiveRoomTypes(Collection<UUID> hotelIds) {
    return hotelDirectory.findActiveRoomTypesByHotelIds(hotelIds).stream()
        .map(rt -> new CatalogRoomType(rt.id(), rt.hotelId(), rt.maxOccupancy(), rt.basePrice()))
        .toList();
  }

  @Override
  public Map<UUID, Long> countOperationalRooms(Collection<UUID> roomTypeIds, Instant at) {
    return hotelDirectory.countOperationalRoomsByRoomTypeIds(roomTypeIds, at);
  }
}
