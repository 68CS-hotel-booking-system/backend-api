package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingHotelAdapter implements PricingHotelPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId) {
    return hotelDirectory.isRoomTypeActiveInHotel(roomTypeId, hotelId);
  }

  @Override
  public Optional<HotelPricingSnapshot> findActiveHotel(UUID hotelId) {
    return hotelDirectory
        .findById(hotelId)
        .filter(HotelDirectory.HotelInfo::active)
        .map(h -> new HotelPricingSnapshot(h.id(), h.checkInTime(), h.checkOutTime()));
  }

  @Override
  public Optional<RoomTypePricingSnapshot> findActiveRoomType(UUID roomTypeId, UUID hotelId) {
    return hotelDirectory
        .findActiveRoomType(roomTypeId, hotelId)
        .map(
            rt ->
                new RoomTypePricingSnapshot(
                    rt.id(),
                    hotelId,
                    rt.name(),
                    rt.maxAdults(),
                    rt.maxChildren(),
                    rt.maxOccupancy(),
                    rt.basePrice()));
  }

  @Override
  public List<RoomTypePricingSnapshot> findActiveRoomTypes(Collection<UUID> roomTypeIds) {
    return hotelDirectory.findActiveRoomTypesByIds(roomTypeIds).stream()
        .map(
            rt ->
                new RoomTypePricingSnapshot(
                    rt.id(),
                    rt.hotelId(),
                    rt.name(),
                    rt.maxAdults(),
                    rt.maxChildren(),
                    rt.maxOccupancy(),
                    rt.basePrice()))
        .toList();
  }

  @Override
  public Optional<HotelManagement> findManagementAccess(
      UUID hotelId, UUID actorUserId, boolean admin) {
    return hotelDirectory
        .findManagementAccess(hotelId, actorUserId, admin)
        .map(view -> new HotelManagement(toLevel(view.level()), toStatus(view.status())));
  }

  private static Level toLevel(HotelDirectory.ManagementLevel level) {
    return switch (level) {
      case ADMIN -> Level.ADMIN;
      case OWNER -> Level.OWNER;
      case MANAGER -> Level.MANAGER;
      case RECEPTIONIST -> Level.RECEPTIONIST;
      case NONE -> Level.NONE;
    };
  }

  private static HotelStatus toStatus(HotelDirectory.HotelStatusView status) {
    return switch (status) {
      case DRAFT -> HotelStatus.DRAFT;
      case ACTIVE -> HotelStatus.ACTIVE;
      case SUSPENDED -> HotelStatus.SUSPENDED;
      case LOCKED -> HotelStatus.LOCKED;
    };
  }
}
