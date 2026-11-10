package com.team.hotelbooking.media.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelMediaDirectory;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelImageAccessAdapter implements HotelImageAccess {

  private final HotelMediaDirectory hotels;

  @Override
  public void requireManage(UUID actorId, UUID hotelId) {
    hotels.requireManage(actorId, hotelId);
  }

  @Override
  public void lockForUpdate(UUID actorId, UUID hotelId, long expectedVersion) {

    hotels.lockForImageUpdate(actorId, hotelId, expectedVersion);
  }

  @Override
  public boolean isPublished(UUID hotelId) {
    return hotels.isPublished(hotelId);
  }
}
