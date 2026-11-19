package com.team.hotelbooking.inventory.domain.repository;

import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomBlockRepository {

  RoomBlock save(RoomBlock roomBlock);

  Optional<RoomBlock> lockByIdAndHotelId(UUID blockId, UUID hotelId);

  List<RoomBlock> findActiveBlocksByRoomIdInRange(
      UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate);

  List<RoomBlock> findActiveBlocksInRange(UUID roomTypeId, LocalDate startDate, LocalDate endDate);

  List<RoomBlock> findManagementBlocks(
      UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate);
}
