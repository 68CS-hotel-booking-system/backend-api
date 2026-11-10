package com.team.hotelbooking.hotel;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface HotelDirectory {

  Optional<HotelInfo> findById(UUID hotelId);

  record HotelInfo(
      UUID id,
      UUID ownerId,
      String name,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      boolean active) {}
}
