package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelCatalogAdapter implements HotelCatalogPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public boolean hasManagementAccess(UUID actorId, UUID hotelId) {
    return hotelDirectory.hasManagementAccess(actorId, hotelId);
  }

  @Override
  public void requireManagementAccess(UUID actorId, UUID hotelId) {
    if (!hotelDirectory.existsById(hotelId)) {
      throw InventoryException.hotelNotFound("Không tìm thấy khách sạn.");
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getAuthorities().stream()
            .anyMatch(
                a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()))) {
      return;
    }
    if (!hotelDirectory.hasManagementAccess(actorId, hotelId)) {
      throw new InventoryException(
          InventoryErrorCode.ACCESS_DENIED, "Bạn không có quyền quản lý khách sạn này.");
    }
  }

  @Override
  public List<UUID> findRoomTypeIdsByHotelId(UUID hotelId) {
    return hotelDirectory.findRoomTypeIdsByHotelId(hotelId);
  }

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

  @Override
  public List<RoomTypeSummary> lockActiveRoomTypesInHotel(
      UUID hotelId, Collection<UUID> roomTypeIds) {
    return hotelDirectory.lockActiveRoomTypesInHotel(hotelId, roomTypeIds).stream()
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

  @Override
  public List<RoomTypeSummary> lockRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds) {
    return hotelDirectory.lockRoomTypesInHotel(hotelId, roomTypeIds).stream()
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

  @Override
  public long countActiveRoomsByRoomTypeId(UUID roomTypeId) {
    return hotelDirectory.countActiveRoomsByRoomTypeId(roomTypeId);
  }

  @Override
  public List<PhysicalRoomSummary> findPhysicalRoomsByRoomTypeId(UUID roomTypeId) {
    return hotelDirectory.findPhysicalRoomsByRoomTypeId(roomTypeId).stream()
        .map(HotelCatalogAdapter::toPhysicalRoomSummary)
        .toList();
  }

  @Override
  public List<PhysicalRoomSummary> findPhysicalRoomsByRoomTypeIds(Collection<UUID> roomTypeIds) {
    return hotelDirectory.findPhysicalRoomsByRoomTypeIds(roomTypeIds).stream()
        .map(HotelCatalogAdapter::toPhysicalRoomSummary)
        .toList();
  }

  @Override
  public List<PhysicalRoomSummary> findPhysicalRoomsByHotelId(UUID hotelId) {
    return hotelDirectory.findPhysicalRoomsByHotelId(hotelId).stream()
        .map(HotelCatalogAdapter::toPhysicalRoomSummary)
        .toList();
  }

  @Override
  public Optional<PhysicalRoomSummary> findPhysicalRoomByIdAndHotelId(UUID roomId, UUID hotelId) {
    return hotelDirectory
        .findPhysicalRoomByIdAndHotelId(roomId, hotelId)
        .map(HotelCatalogAdapter::toPhysicalRoomSummary);
  }

  @Override
  public Optional<PhysicalRoomSummary> updateRoomReadiness(
      UUID roomId, UUID hotelId, String readiness, UUID actorUserId, Instant updatedAt) {
    return hotelDirectory
        .updateRoomReadiness(roomId, hotelId, readiness, actorUserId, updatedAt)
        .map(HotelCatalogAdapter::toPhysicalRoomSummary);
  }

  private static PhysicalRoomSummary toPhysicalRoomSummary(HotelDirectory.PhysicalRoomView r) {
    return new PhysicalRoomSummary(
        r.id(),
        r.hotelId(),
        r.roomTypeId(),
        r.roomNumber(),
        r.floor(),
        r.readiness(),
        r.operationalFrom(),
        r.operationalUntil());
  }
}
