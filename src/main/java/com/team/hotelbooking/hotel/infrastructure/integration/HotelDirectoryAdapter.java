package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelDirectoryAdapter implements HotelDirectory {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;
  private final RoomJpaRepository roomJpaRepository;

  @Override
  public Optional<HotelInfo> findById(UUID hotelId) {
    return hotelJpaRepository
        .findById(hotelId)
        .map(
            h ->
                new HotelInfo(
                    h.getId(),
                    h.getOwnerId(),
                    h.getName(),
                    h.getCheckInTime(),
                    h.getCheckOutTime(),
                    h.isActive()));
  }

  @Override
  public boolean existsById(UUID hotelId) {
    return hotelJpaRepository.existsById(hotelId);
  }

  @Override
  public boolean isHotelActive(UUID hotelId) {
    return hotelJpaRepository.findByIdAndActiveTrue(hotelId).isPresent();
  }

  @Override
  public Optional<UUID> findOwnerUserId(UUID hotelId) {
    return hotelJpaRepository.findById(hotelId).map(HotelJpaEntity::getOwnerUserId);
  }

  @Override
  public Optional<HotelSummaryView> findHotelSummary(UUID hotelId) {
    return hotelJpaRepository
        .findByIdAndActiveTrue(hotelId)
        .map(h -> new HotelSummaryView(h.getId(), h.isActive(), false));
  }

  @Override
  public List<RoomTypeSummaryView> findActiveRoomTypes(UUID hotelId) {
    return roomTypeJpaRepository.findAllByHotelIdAndActiveTrue(hotelId).stream()
        .map(
            rt ->
                new RoomTypeSummaryView(
                    rt.getId(),
                    rt.getName(),
                    (int) rt.getMaxOccupancy(),
                    (int) rt.getMaxAdults(),
                    (int) rt.getMaxChildren(),
                    rt.getBasePrice()))
        .toList();
  }

  @Override
  public boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository.findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId).isPresent();
  }

  @Override
  public Optional<RoomTypeSummaryView> findActiveRoomType(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository
        .findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId)
        .map(
            rt ->
                new RoomTypeSummaryView(
                    rt.getId(),
                    rt.getName(),
                    (int) rt.getMaxOccupancy(),
                    (int) rt.getMaxAdults(),
                    (int) rt.getMaxChildren(),
                    rt.getBasePrice()));
  }

  @Override
  public long countActiveRoomsByRoomTypeId(UUID roomTypeId) {
    return roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId);
  }
}
