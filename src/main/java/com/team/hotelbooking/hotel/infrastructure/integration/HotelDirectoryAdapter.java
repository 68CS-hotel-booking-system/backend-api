package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.util.Optional;
import java.util.UUID;

public class HotelDirectoryAdapter implements HotelDirectory {

  private final HotelRepository hotels;

  public HotelDirectoryAdapter(HotelRepository hotels) {
    this.hotels = hotels;
  }

  @Override
  public Optional<HotelInfo> findById(UUID hotelId) {
    return hotels
        .findById(hotelId)
        .map(
            hotel ->
                new HotelInfo(
                    hotel.id(),
                    hotel.ownerId(),
                    hotel.name(),
                    hotel.checkInTime(),
                    hotel.checkOutTime(),
                    hotel.status() == Hotel.Status.ACTIVE));
  }
}
