package com.team.hotelbooking.inventory.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Dữ liệu khách sạn cho tìm kiếm, lấy theo lô từ module hotel.
public interface HotelSearchCatalogPort {

  // Chỉ khách sạn ACTIVE; có đủ mọi tiện ích khách sạn trong amenityIds.
  List<SearchableHotel> findSearchableHotels(
      UUID cityId, String citySlug, Integer minStarRating, Set<UUID> amenityIds);

  List<CatalogRoomType> findActiveRoomTypes(Collection<UUID> hotelIds);

  Map<UUID, Long> countOperationalRooms(Collection<UUID> roomTypeIds, Instant at);

  record SearchableHotel(
      UUID id,
      String name,
      String slug,
      String address,
      Short starRating,
      City city,
      List<Amenity> amenities) {

    public SearchableHotel {
      amenities = List.copyOf(amenities);
    }
  }

  record City(UUID id, String name, String slug) {}

  record Amenity(UUID id, String code, String name) {}

  record CatalogRoomType(UUID id, UUID hotelId, int maxOccupancy, long basePrice) {}
}
