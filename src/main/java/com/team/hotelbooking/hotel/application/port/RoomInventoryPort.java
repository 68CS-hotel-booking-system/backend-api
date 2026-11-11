package com.team.hotelbooking.hotel.application.port;

import java.util.UUID;

public interface RoomInventoryPort {
  void lockRoomType(UUID hotelId, UUID roomTypeId);

  void requireAllocatableAfterDeactivation(UUID roomTypeId, UUID roomId);
}
