package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelCatalogAdapter implements HotelCatalogPort {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;

  @Override
  public Optional<HotelSummary> findHotelSummary(UUID hotelId) {
    return hotelJpaRepository
        .findByIdAndActiveTrue(hotelId)
        .map(hotel -> new HotelSummary(hotel.getId(), hotel.isActive(), false));
  }

  @Override
  public List<RoomTypeSummary> findActiveRoomTypes(UUID hotelId) {
    List<RoomTypeJpaEntity> roomTypes =
        roomTypeJpaRepository.findAllByHotelIdAndActiveTrue(hotelId);

    return roomTypes.stream()
        .map(
            rt ->
                new RoomTypeSummary(
                    rt.getId(),
                    rt.getName(),
                    (int) rt.getMaxOccupancy(),
                    (int) rt.getMaxAdults(),
                    (int) rt.getMaxChildren(),
                    rt.getBasePrice()))
        .toList();
  }
}
