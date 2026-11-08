package com.team.hotelbooking.hotel;

import java.util.UUID;

public interface HotelMediaDirectory {

  void requireManage(UUID actorId, UUID hotelId);

  void lockForImageUpdate(UUID actorId, UUID hotelId, long expectedVersion);

  boolean isPublished(UUID hotelId);
}
