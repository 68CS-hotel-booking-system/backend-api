package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.ListRoomBlocksQuery;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.usecase.ListRoomBlocksUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import java.util.List;

public class ListRoomBlocksService implements ListRoomBlocksUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final RoomBlockRepository roomBlockRepository;

  public ListRoomBlocksService(
      HotelCatalogPort hotelCatalogPort, RoomBlockRepository roomBlockRepository) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.roomBlockRepository = roomBlockRepository;
  }

  @Override
  public List<RoomBlockResult> execute(ListRoomBlocksQuery query) {
    if (query == null || query.hotelId() == null) {
      throw InventoryException.invalidDateRange("Mã khách sạn không được để trống.");
    }

    if (query.startDate() != null
        && query.endDate() != null
        && query.endDate().isBefore(query.startDate())) {
      throw InventoryException.invalidDateRange("Ngày kết thúc không được trước ngày bắt đầu.");
    }

    hotelCatalogPort
        .findHotelSummary(query.hotelId())
        .orElseThrow(() -> InventoryException.hotelNotFound("Không tìm thấy khách sạn."));
    if (query.actorId() != null) {
      hotelCatalogPort.requireManagementAccess(query.actorId(), query.hotelId());
    }

    return roomBlockRepository
        .findManagementBlocks(
            query.hotelId(), query.roomTypeId(), query.roomId(), query.startDate(), query.endDate())
        .stream()
        .map(RoomBlockResult::from)
        .toList();
  }
}
