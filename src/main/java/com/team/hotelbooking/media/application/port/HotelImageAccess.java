package com.team.hotelbooking.media.application.port;

import java.util.UUID;

public interface HotelImageAccess {

  void requireManage(UUID actorId, UUID hotelId);

  void requireUpload(UUID actorId, UUID hotelId);

  void lockForUpdate(UUID actorId, UUID hotelId, long expectedVersion);

  boolean isPublished(UUID hotelId);

  boolean roomTypeExists(UUID hotelId, UUID roomTypeId);

  boolean isRoomTypePublished(UUID hotelId, UUID roomTypeId);

  void lockRoomTypeForImageUpdate(UUID actorId, UUID hotelId, UUID roomTypeId);
}
