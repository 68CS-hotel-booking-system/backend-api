package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.CustomAmenity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ScopedAmenityRepository {
  List<Amenity> findCatalogByRoomId(UUID roomId);

  void replaceRoomCatalog(UUID roomId, Set<UUID> amenityIds);

  List<CustomAmenity> findCustomByScope(UUID hotelId, UUID roomId);

  Optional<CustomAmenity> findCustomByIdAndScope(UUID id, UUID hotelId, UUID roomId);

  boolean existsOtherCustomName(UUID hotelId, UUID roomId, String normalizedName, UUID excludedId);

  CustomAmenity addCustom(CustomAmenity amenity);

  CustomAmenity updateCustom(CustomAmenity amenity);
}
