package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.RoomInventoryDirectory;
import com.team.hotelbooking.hotel.application.port.RoomInventoryPort;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomInventoryAdapter implements RoomInventoryPort {
  private final RoomInventoryDirectory inventory;

  @Override
  public void lockRoomType(UUID hotelId, UUID roomTypeId) {
    try {
      inventory.lockRoomType(hotelId, roomTypeId);
    } catch (RoomInventoryDirectory.RoomChangeConflict ex) {
      throw new HotelException(HotelErrorCode.ROOM_OPERATION_CONFLICT, ex.getMessage());
    }
  }

  @Override
  public void requireAllocatableAfterDeactivation(UUID roomTypeId, UUID roomId) {
    try {
      inventory.requireAllocatableAfterDeactivation(roomTypeId, roomId);
    } catch (RoomInventoryDirectory.RoomChangeConflict ex) {
      throw new HotelException(HotelErrorCode.ROOM_OPERATION_CONFLICT, ex.getMessage());
    }
  }
}
