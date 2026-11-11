package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {

  Room add(Room room);

  Optional<Room> findByIdAndHotelId(UUID roomId, UUID hotelId);

  // So khớp số phòng chính xác (phân biệt hoa thường), bỏ qua chính phòng đang sửa.
  boolean existsOtherRoomWithNumber(UUID hotelId, String roomNumber, UUID excludedRoomId);

  Room update(Room room);

  // HT-03: số phòng đang phục vụ tại thời điểm at theo loại phòng; loại không có phòng thì vắng
  // mặt.
  Map<UUID, Long> countOperationalRoomsByRoomType(UUID hotelId, Instant at);

  PageResult<Room> findByHotelId(UUID hotelId, PageQuery pagination);

  PageResult<Room> findByHotelIdAndRoomTypeId(UUID hotelId, UUID roomTypeId, PageQuery pagination);
}
