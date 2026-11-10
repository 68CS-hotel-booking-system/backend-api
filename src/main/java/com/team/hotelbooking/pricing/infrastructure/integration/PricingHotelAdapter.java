package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingHotelAdapter implements PricingHotelPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public boolean hasManagementAccess(UUID actorId, UUID hotelId) {
    return hotelDirectory.hasManagementAccess(actorId, hotelId);
  }

  @Override
  public void requireManagementAccess(UUID actorId, UUID hotelId) {
    if (!hotelDirectory.existsById(hotelId)) {
      throw new PricingException(PricingErrorCode.HOTEL_NOT_FOUND);
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getAuthorities().stream()
            .anyMatch(
                a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()))) {
      return;
    }
    if (!hotelDirectory.hasManagementAccess(actorId, hotelId)) {
      throw new PricingException(PricingErrorCode.ACCESS_DENIED);
    }
  }

  @Override
  public boolean isHotelActive(UUID hotelId) {
    return hotelDirectory.isHotelActive(hotelId);
  }

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
}
