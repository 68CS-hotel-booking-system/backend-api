package com.team.hotelbooking.hotel;

import java.util.UUID;

public interface HotelMediaDirectory {

  void requireManage(UUID actorId, UUID hotelId);

  void requireImageUpload(UUID actorId, UUID hotelId);

  void lockForImageUpdate(UUID actorId, UUID hotelId, long expectedVersion);

  boolean isPublished(UUID hotelId);

  boolean roomTypeExists(UUID hotelId, UUID roomTypeId);

  boolean isRoomTypePublished(UUID hotelId, UUID roomTypeId);

  void lockRoomTypeForImageUpdate(UUID actorId, UUID hotelId, UUID roomTypeId);
}
