package com.team.hotelbooking.hotel;

import java.util.UUID;

/** Kiểm tra thay đổi phòng vật lý trong transaction của bên gọi. */
public interface RoomInventoryDirectory {
  void lockRoomType(UUID hotelId, UUID roomTypeId);

  void requireAllocatableAfterDeactivation(UUID roomTypeId, UUID roomId);

  final class RoomChangeConflict extends RuntimeException {
    public RoomChangeConflict(String message) {
      super(message);
    }
  }
}
