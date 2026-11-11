package com.team.hotelbooking.hotel;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelDirectory {

  // Phục vụ module booking & pricing
  Optional<HotelInfo> findById(UUID hotelId);

  // Phục vụ module payment
  boolean existsById(UUID hotelId);

  Optional<UUID> findOwnerUserId(UUID hotelId);

  // Phục vụ module pricing
  boolean isHotelActive(UUID hotelId);

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);

  Optional<RoomTypeSummaryView> findActiveRoomType(UUID roomTypeId, UUID hotelId);

  // Phục vụ module inventory & pricing: kiểm tra quyền quản lý khách sạn
  boolean hasManagementAccess(UUID actorId, UUID hotelId);

  // Phục vụ module inventory
  Optional<HotelSummaryView> findHotelSummary(UUID hotelId);

  List<UUID> findRoomTypeIdsByHotelId(UUID hotelId);

  List<RoomTypeSummaryView> findActiveRoomTypes(UUID hotelId);

  List<RoomTypeSummaryView> lockActiveRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  List<RoomTypeSummaryView> lockRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  long countActiveRoomsByRoomTypeId(UUID roomTypeId);

  List<PhysicalRoomView> findPhysicalRoomsByRoomTypeId(UUID roomTypeId);

  List<PhysicalRoomView> findPhysicalRoomsByHotelId(UUID hotelId);

  Optional<PhysicalRoomView> findPhysicalRoomByIdAndHotelId(UUID roomId, UUID hotelId);

  Optional<PhysicalRoomView> updateRoomReadiness(
      UUID roomId, UUID hotelId, String readiness, UUID actorUserId, Instant updatedAt);

  // DTO Records dùng chung
  record HotelInfo(
      UUID id,
      UUID ownerId,
      String name,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      boolean active) {}

  record HotelSummaryView(UUID id, boolean active, boolean verified) {}

  record RoomTypeSummaryView(
      UUID id, String name, int maxOccupancy, int maxAdults, int maxChildren, long basePrice) {}

  record PhysicalRoomView(
      UUID id,
      UUID hotelId,
      UUID roomTypeId,
      String roomNumber,
      String floor,
      String readiness,
      Instant operationalFrom,
      Instant operationalUntil) {

    public PhysicalRoomView(
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
}
