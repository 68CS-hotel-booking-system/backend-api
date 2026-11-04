package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.UUID;

public interface RoomRepository {

  Room add(Room room);

  PageResult<Room> findByHotelId(UUID hotelId, PageQuery pagination);

  PageResult<Room> findByHotelIdAndRoomTypeId(UUID hotelId, UUID roomTypeId, PageQuery pagination);
}
