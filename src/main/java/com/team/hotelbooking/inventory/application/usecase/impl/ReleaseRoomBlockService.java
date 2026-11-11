package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.ReleaseRoomBlockCommand;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.usecase.ReleaseRoomBlockUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import java.time.Clock;
import java.time.Instant;

public class ReleaseRoomBlockService implements ReleaseRoomBlockUseCase {

  private final RoomBlockRepository roomBlockRepository;
  private final HotelCatalogPort hotelCatalogPort;
  private final Clock clock;

  public ReleaseRoomBlockService(
      RoomBlockRepository roomBlockRepository, HotelCatalogPort hotelCatalogPort, Clock clock) {
    this.roomBlockRepository = roomBlockRepository;
    this.hotelCatalogPort = hotelCatalogPort;
    this.clock = clock;
  }

  public ReleaseRoomBlockService(RoomBlockRepository roomBlockRepository, Clock clock) {
    this(roomBlockRepository, null, clock);
  }

  @Override
  public RoomBlockResult execute(ReleaseRoomBlockCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.blockId() == null) {
      throw InventoryException.invalidDateRange(
          "Thông tin người thực hiện và mã lịch chặn không được để trống.");
    }

    if (hotelCatalogPort != null) {
      hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());
    }

    if (command.reason() == null || command.reason().trim().isEmpty()) {
      throw InventoryException.invalidDateRange(
          "Lý do giải phóng lịch chặn không được để trống (BL-10).");
    }

    RoomBlock existing =
        roomBlockRepository
            .lockByIdAndHotelId(command.blockId(), command.hotelId())
            .orElseThrow(
                () -> InventoryException.roomBlockNotFound("Không tìm thấy lịch chặn phòng."));

    // Gọi lặp cùng thao tác không cộng tồn thêm lần nữa (idempotent theo BL-10)
    if (existing.status() == RoomBlock.Status.RELEASED) {
      return RoomBlockResult.from(existing);
    }

    Instant now = clock.instant();
    RoomBlock released =
        new RoomBlock(
            existing.id(),
            existing.hotelId(),
            existing.roomTypeId(),
            existing.roomId(),
            existing.startDate(),
            existing.endDate(),
            existing.reason(),
            RoomBlock.Status.RELEASED,
            existing.createdByUserId(),
            existing.createdAt(),
            command.actorId(),
            now,
            command.actorId(),
            now,
            command.reason().trim());

    RoomBlock saved = roomBlockRepository.save(released);
    return RoomBlockResult.from(saved);
  }
}
