package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository {

  RoomType add(RoomType roomType);

  Optional<RoomType> findByIdAndHotelId(UUID roomTypeId, UUID hotelId);

  List<RoomType> findActiveByHotelId(UUID hotelId);

  List<RoomType> findByHotelId(UUID hotelId);
}
