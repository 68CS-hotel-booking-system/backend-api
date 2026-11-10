package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.time.Instant;
import java.util.Collection;
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
  public List<RoomTypeSummaryView> lockActiveRoomTypesInHotel(
      UUID hotelId, Collection<UUID> roomTypeIds) {
    if (roomTypeIds == null || roomTypeIds.isEmpty()) {
      return List.of();
    }
    return roomTypeJpaRepository
        .lockActiveByHotelIdAndIdInOrderByIdAsc(hotelId, roomTypeIds)
        .stream()
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
  public long countActiveRoomsByRoomTypeId(UUID roomTypeId) {
    return roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId);
  }

  @Override
  public List<PhysicalRoomView> findPhysicalRoomsByRoomTypeId(UUID roomTypeId) {
    return roomJpaRepository.findAllByRoomTypeIdOrderByIdAsc(roomTypeId).stream()
        .map(HotelDirectoryAdapter::toPhysicalRoomView)
        .toList();
  }

  @Override
  public List<PhysicalRoomView> findPhysicalRoomsByHotelId(UUID hotelId) {
    return roomJpaRepository.findAllByHotelIdOrdered(hotelId).stream()
        .map(HotelDirectoryAdapter::toPhysicalRoomView)
        .toList();
  }

  @Override
  public Optional<PhysicalRoomView> findPhysicalRoomByIdAndHotelId(UUID roomId, UUID hotelId) {
    return roomJpaRepository
        .findByIdAndHotelId(roomId, hotelId)
        .map(HotelDirectoryAdapter::toPhysicalRoomView);
  }

  @Override
  public Optional<PhysicalRoomView> updateRoomReadiness(
      UUID roomId, UUID hotelId, String readiness, UUID actorUserId, Instant updatedAt) {
    return roomJpaRepository
        .findByIdAndHotelId(roomId, hotelId)
        .map(
            room -> {
              room.setReadiness(RoomJpaEntity.DbReadiness.valueOf(readiness));
              room.setReadinessUpdatedBy(actorUserId);
              room.setReadinessUpdatedAt(updatedAt);
              room.setUpdatedAt(updatedAt);
              return toPhysicalRoomView(roomJpaRepository.save(room));
            });
  }

  private static PhysicalRoomView toPhysicalRoomView(RoomJpaEntity r) {
    return new PhysicalRoomView(
        r.getId(),
        r.getHotelId(),
        r.getRoomTypeId(),
        r.getRoomNumber(),
        r.getFloor(),
        r.getReadiness() != null ? r.getReadiness().name() : "READY",
        r.getOperationalFrom(),
        r.getOperationalUntil());
  }
}
