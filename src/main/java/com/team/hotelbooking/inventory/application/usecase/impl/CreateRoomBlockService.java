package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.CreateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.usecase.CreateRoomBlockUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class CreateRoomBlockService implements CreateRoomBlockUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final RoomBlockRepository roomBlockRepository;
  private final InventoryQueryPort inventoryQueryPort;
  private final Clock clock;

  public CreateRoomBlockService(
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
  public RoomBlockResult execute(CreateRoomBlockCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.roomId() == null
        || command.startDate() == null
        || command.endDate() == null) {
      throw InventoryException.invalidDateRange(
          "Thông tin phòng vật lý và khoảng ngày chặn không được để trống.");
    }

    if (command.reason() == null || command.reason().trim().isEmpty()) {
      throw InventoryException.invalidDateRange("Lý do chặn phòng không được để trống.");
    }

    if (command.endDate().isBefore(command.startDate())) {
      throw InventoryException.invalidDateRange(
          "Ngày kết thúc chặn phòng không được trước ngày bắt đầu.");
    }

    HotelCatalogPort.HotelSummary hotel =
        hotelCatalogPort
            .findHotelSummary(command.hotelId())
            .orElseThrow(() -> InventoryException.hotelNotFound("Không tìm thấy khách sạn."));

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    if (!hotel.isActive() || hotel.isSuspended()) {
      throw InventoryException.hotelNotFound("Khách sạn hiện đang tạm ngừng kinh doanh.");
    }

    HotelCatalogPort.PhysicalRoomSummary room =
        hotelCatalogPort
            .findPhysicalRoomByIdAndHotelId(command.roomId(), command.hotelId())
            .orElseThrow(
                () ->
                    InventoryException.roomNotFound(
                        "Không tìm thấy phòng vật lý thuộc khách sạn này."));

    // Khóa hàng room_types để tuần tự hóa với luồng giữ chỗ và cập nhật lịch chặn (IV-08, BL-06)
    List<HotelCatalogPort.RoomTypeSummary> lockedTypes =
        hotelCatalogPort.lockActiveRoomTypesInHotel(command.hotelId(), List.of(room.roomTypeId()));
    if (lockedTypes.isEmpty()) {
      throw InventoryException.roomTypeInactive(
          "Loại phòng của phòng vật lý không tồn tại hoặc đã ngừng bán.");
    }

    // Lấy thời điểm now sau khi đã lấy khóa theo AgentRules §5
    Instant now = clock.instant();
    LocalDate today = BusinessTime.today(clock);

    if (command.startDate().isBefore(today)) {
      throw InventoryException.invalidDateRange(
          "Không được tạo lịch chặn cho các đêm đã qua (BL-04).");
    }

    if (room.operationalUntil() != null && !room.operationalUntil().isAfter(now)) {
      throw InventoryException.roomNotFound("Phòng vật lý đã ngừng phục vụ.");
    }

    // Kiểm tra chống chồng lấn hai lịch ACTIVE cho cùng một phòng vật lý (BL-09)
    List<RoomBlock> overlappingBlocks =
        roomBlockRepository.findActiveBlocksByRoomIdInRange(
            room.roomTypeId(), room.id(), command.startDate(), command.endDate());
    if (!overlappingBlocks.isEmpty()) {
      throw InventoryException.roomBlockOverlap(
          "Phòng vật lý đã có lịch chặn đang hoạt động chồng lấn khoảng ngày này.");
    }

    // Kiểm tra khả năng phân bổ liên tục cho mọi đơn đang giữ hợp lệ và đã xác nhận (BL-06, IV-13)
    boolean canStillAllocate =
        inventoryQueryPort.canAllocateWithCandidateBlock(
            room.roomTypeId(), null, room.id(), command.startDate(), command.endDate());
    if (!canStillAllocate) {
      throw InventoryException.roomBlockConflictsReservation(
          "Không thể chặn phòng vì làm mất khả năng phân bổ phòng liên tục cho các đơn đang giữ hoặc đã xác nhận.");
    }

    RoomBlock toSave =
        new RoomBlock(
            null,
            command.hotelId(),
            room.roomTypeId(),
            room.id(),
            command.startDate(),
            command.endDate(),
            command.reason().trim(),
            RoomBlock.Status.ACTIVE,
            command.actorId(),
            now,
            command.actorId(),
            now,
            null,
            null,
            null);

    RoomBlock saved = roomBlockRepository.save(toSave);
    return RoomBlockResult.from(saved);
  }
}
