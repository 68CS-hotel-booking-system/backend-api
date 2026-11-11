package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.mapper.RoomAssignmentPersistenceMapper;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomAssignmentJpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoomAssignmentRepositoryAdapter implements RoomAssignmentRepository {

  private final RoomAssignmentJpaRepository roomAssignmentJpaRepository;
  private final RoomAssignmentPersistenceMapper mapper;

  @Override
  public RoomAssignment save(RoomAssignment assignment) {
    RoomAssignmentJpaEntity saved = roomAssignmentJpaRepository.save(mapper.toEntity(assignment));
    return mapper.toDomain(saved);
  }

  @Override
  public List<RoomAssignment> saveAll(List<RoomAssignment> assignments) {
    if (assignments == null || assignments.isEmpty()) {
      return List.of();
    }
    List<RoomAssignmentJpaEntity> entities = assignments.stream().map(mapper::toEntity).toList();
    return roomAssignmentJpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<RoomAssignment> findActiveByBookingId(UUID bookingId) {
    return roomAssignmentJpaRepository.findActiveByBookingId(bookingId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomAssignment> findAllByBookingId(UUID bookingId) {
    return roomAssignmentJpaRepository.findAllByBookingId(bookingId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomAssignment> findActiveByRoomIdOverlapping(
      UUID roomId, LocalDate startDate, LocalDate endDate) {
    return roomAssignmentJpaRepository
        .findActiveByRoomIdOverlapping(roomId, startDate, endDate)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomAssignment> findActiveByHotelIdOverlapping(
      UUID hotelId, LocalDate startDate, LocalDate endDate) {
    return roomAssignmentJpaRepository
        .findActiveByHotelIdOverlapping(hotelId, startDate, endDate)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomAssignment> findActiveByRoomTypeIdsOverlapping(
      Collection<UUID> roomTypeIds, LocalDate startDate, LocalDate endDate) {
    if (roomTypeIds == null || roomTypeIds.isEmpty()) {
      return List.of();
    }
    return roomAssignmentJpaRepository
        .findActiveByRoomTypeIdsOverlapping(roomTypeIds, startDate, endDate)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
