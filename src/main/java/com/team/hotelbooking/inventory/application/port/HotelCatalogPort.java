package com.team.hotelbooking.inventory.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelCatalogPort {

  record HotelSummary(UUID id, boolean isActive, boolean isSuspended) {}

  record RoomTypeSummary(
      UUID id, String name, int maxOccupancy, int maxAdults, int maxChildren, Long basePrice) {}

  record PhysicalRoomSummary(
      UUID id,
      UUID hotelId,
      UUID roomTypeId,
      String roomNumber,
      String floor,
      String readiness,
      Instant operationalFrom,
      Instant operationalUntil) {

    public PhysicalRoomSummary(
        UUID id,
        UUID hotelId,
        UUID roomTypeId,
        String roomNumber,
        String floor,
        Instant operationalFrom,
        Instant operationalUntil) {
      this(id, hotelId, roomTypeId, roomNumber, floor, "READY", operationalFrom, operationalUntil);
    }
  }

  Optional<HotelSummary> findHotelSummary(UUID hotelId);

  boolean hasManagementAccess(UUID actorId, UUID hotelId);

  void requireManagementAccess(UUID actorId, UUID hotelId);

  List<UUID> findRoomTypeIdsByHotelId(UUID hotelId);

  List<RoomTypeSummary> findActiveRoomTypes(UUID hotelId);

  List<RoomTypeSummary> lockActiveRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  List<RoomTypeSummary> lockRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  long countActiveRoomsByRoomTypeId(UUID roomTypeId);

  List<PhysicalRoomSummary> findPhysicalRoomsByRoomTypeId(UUID roomTypeId);

  List<PhysicalRoomSummary> findPhysicalRoomsByRoomTypeIds(Collection<UUID> roomTypeIds);

  List<PhysicalRoomSummary> findPhysicalRoomsByHotelId(UUID hotelId);

  Optional<PhysicalRoomSummary> findPhysicalRoomByIdAndHotelId(UUID roomId, UUID hotelId);

  Optional<PhysicalRoomSummary> updateRoomReadiness(
      UUID roomId, UUID hotelId, String readiness, UUID actorUserId, Instant updatedAt);
}
