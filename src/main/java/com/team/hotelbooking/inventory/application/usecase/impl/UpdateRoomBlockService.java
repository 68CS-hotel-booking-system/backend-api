package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.UpdateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomBlockUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class UpdateRoomBlockService implements UpdateRoomBlockUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final RoomBlockRepository roomBlockRepository;
  private final InventoryQueryPort inventoryQueryPort;
  private final Clock clock;

  public UpdateRoomBlockService(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.roomBlockRepository = roomBlockRepository;
    this.inventoryQueryPort = inventoryQueryPort;
    this.clock = clock;
  }

  @Override
  public RoomBlockResult execute(UpdateRoomBlockCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.blockId() == null
        || command.startDate() == null
        || command.endDate() == null) {
      throw InventoryException.invalidDateRange(
          "Thông tin lịch chặn và khoảng ngày không được để trống.");
    }

    if (command.reason() == null || command.reason().trim().isEmpty()) {
      throw InventoryException.invalidDateRange("Lý do cập nhật lịch chặn không được để trống.");
    }

    if (command.endDate().isBefore(command.startDate())) {
      throw InventoryException.invalidDateRange(
          "Ngày kết thúc chặn phòng không được trước ngày bắt đầu.");
    }

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    RoomBlock existing =
        roomBlockRepository
            .lockByIdAndHotelId(command.blockId(), command.hotelId())
            .orElseThrow(
                () -> InventoryException.roomBlockNotFound("Không tìm thấy lịch chặn phòng."));

    if (existing.status() == RoomBlock.Status.RELEASED) {
      throw InventoryException.roomBlockAlreadyReleased(
          "Lịch chặn phòng đã được giải phóng trước đó, không thể chỉnh sửa.");
    }

    hotelCatalogPort.lockActiveRoomTypesInHotel(command.hotelId(), List.of(existing.roomTypeId()));

    Instant now = clock.instant();
    LocalDate today = BusinessTime.today(clock);

    // Không cho sửa lịch làm thay đổi tồn lịch sử của các đêm đã qua (BL-04)
    if (existing.startDate().isBefore(today)) {
      if (!command.startDate().equals(existing.startDate())) {
        throw InventoryException.invalidDateRange(
            "Không được thay đổi ngày bắt đầu đã qua của lịch chặn đang diễn ra (BL-04).");
      }
      if (command.endDate().isBefore(today)) {
        throw InventoryException.invalidDateRange(
            "Ngày kết thúc lịch chặn không được lùi về quá khứ (BL-04).");
      }
    } else if (command.startDate().isBefore(today)) {
      throw InventoryException.invalidDateRange(
          "Không được dời ngày bắt đầu lịch chặn về quá khứ (BL-04).");
    }

    // Kiểm tra chống chồng lấn với các lịch ACTIVE khác của cùng phòng (BL-09)
    List<RoomBlock> overlappingBlocks =
        roomBlockRepository
            .findActiveBlocksByRoomIdInRange(
                existing.roomTypeId(), existing.roomId(), command.startDate(), command.endDate())
            .stream()
            .filter(b -> !existing.id().equals(b.id()))
            .toList();
    if (!overlappingBlocks.isEmpty()) {
      throw InventoryException.roomBlockOverlap(
          "Phòng vật lý đã có lịch chặn đang hoạt động khác chồng lấn khoảng ngày này.");
    }

    // Kiểm tra khả năng phân bổ liên tục cho các đơn đang giữ và đã xác nhận (BL-06, IV-13)
    boolean canStillAllocate =
        inventoryQueryPort.canAllocateWithCandidateBlock(
            existing.roomTypeId(),
            existing.id(),
            existing.roomId(),
            command.startDate(),
            command.endDate());
    if (!canStillAllocate) {
      throw InventoryException.roomBlockConflictsReservation(
          "Không thể gia hạn/sửa lịch chặn vì làm mất khả năng phân bổ phòng liên tục cho các đơn đang giữ hoặc đã xác nhận.");
    }

    RoomBlock updated =
        new RoomBlock(
            existing.id(),
            existing.hotelId(),
            existing.roomTypeId(),
            existing.roomId(),
            command.startDate(),
            command.endDate(),
            command.reason().trim(),
            RoomBlock.Status.ACTIVE,
            existing.createdByUserId(),
            existing.createdAt(),
            command.actorId(),
            now,
            null,
            null,
            null);

    RoomBlock saved = roomBlockRepository.save(updated);
    return RoomBlockResult.from(saved);
  }
}
