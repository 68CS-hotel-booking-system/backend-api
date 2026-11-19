package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.UpdateRoomReadinessCommand;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomReadinessUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

public class UpdateRoomReadinessService implements UpdateRoomReadinessUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final Clock clock;

  public UpdateRoomReadinessService(HotelCatalogPort hotelCatalogPort, Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.clock = clock;
  }

  @Override
  public RoomReadinessResult execute(UpdateRoomReadinessCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.roomId() == null
        || command.readiness() == null
        || command.readiness().trim().isEmpty()) {
      throw InventoryException.invalidStayOperation(
          "Thông tin phòng vật lý và trạng thái buồng phòng không được để trống.");
    }

    String normalized = command.readiness().trim().toUpperCase(Locale.ROOT);
    if (!"READY".equals(normalized) && !"DIRTY".equals(normalized)) {
      throw InventoryException.invalidStayOperation(
          "Trạng thái buồng phòng chỉ chấp nhận giá trị READY hoặc DIRTY.");
    }

    hotelCatalogPort
        .findHotelSummary(command.hotelId())
        .orElseThrow(() -> InventoryException.hotelNotFound("Không tìm thấy khách sạn."));
    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    Instant now = clock.instant();
    PhysicalRoomSummary updated =
        hotelCatalogPort
            .updateRoomReadiness(
                command.roomId(), command.hotelId(), normalized, command.actorId(), now)
            .orElseThrow(
                () ->
                    InventoryException.roomNotFound(
                        "Không tìm thấy phòng vật lý thuộc khách sạn này."));

    return new RoomReadinessResult(
        command.hotelId(),
        updated.id(),
        updated.roomTypeId(),
        updated.roomNumber(),
        updated.floor(),
        updated.readiness(),
        now);
  }
}
