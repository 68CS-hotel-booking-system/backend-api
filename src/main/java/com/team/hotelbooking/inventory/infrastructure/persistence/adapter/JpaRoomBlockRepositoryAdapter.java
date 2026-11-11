package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.mapper.RoomBlockPersistenceMapper;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaRoomBlockRepositoryAdapter implements RoomBlockRepository {

  private final RoomBlockJpaRepository roomBlockJpaRepository;

  public JpaRoomBlockRepositoryAdapter(RoomBlockJpaRepository roomBlockJpaRepository) {
    this.roomBlockJpaRepository = roomBlockJpaRepository;
  }

  @Override
  public RoomBlock save(RoomBlock roomBlock) {
    RoomBlockJpaEntity saved =
        roomBlockJpaRepository.save(RoomBlockPersistenceMapper.toEntity(roomBlock));
    return RoomBlockPersistenceMapper.toDomain(saved);
  }

  @Override
  public Optional<RoomBlock> lockByIdAndHotelId(UUID blockId, UUID hotelId) {
    return roomBlockJpaRepository
        .lockByIdAndHotelId(blockId, hotelId)
        .map(RoomBlockPersistenceMapper::toDomain);
  }

  @Override
  public List<RoomBlock> findActiveBlocksByRoomIdInRange(
      UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate) {
    return roomBlockJpaRepository
        .findActiveBlocksInRange(roomTypeId, startDate, endDate.plusDays(1))
        .stream()
        .filter(b -> roomId.equals(b.getRoomId()))
        .map(RoomBlockPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomBlock> findActiveBlocksInRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    return roomBlockJpaRepository
        .findActiveBlocksInRange(roomTypeId, startDate, endDate.plusDays(1))
        .stream()
        .map(RoomBlockPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomBlock> findManagementBlocks(
      UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate) {
    return roomBlockJpaRepository.findByHotelIdOrderByStartDateAscEndDateAscIdAsc(hotelId).stream()
        .filter(b -> roomTypeId == null || roomTypeId.equals(b.getRoomTypeId()))
        .filter(b -> roomId == null || roomId.equals(b.getRoomId()))
        .filter(b -> startDate == null || !b.getEndDate().isBefore(startDate))
        .filter(b -> endDate == null || !b.getStartDate().isAfter(endDate))
        .map(RoomBlockPersistenceMapper::toDomain)
        .toList();
  }
}
