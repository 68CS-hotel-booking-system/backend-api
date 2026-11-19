package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import java.util.Optional;
import java.util.UUID;

public interface HotelStatusHistoryRepository {

  void add(HotelStatusChange change);

  // Lần gần nhất khách sạn được chuyển vào trạng thái toStatus (vd lần khóa gần nhất).
  Optional<HotelStatusChange> findLatestInto(UUID hotelId, Hotel.Status toStatus);
}
