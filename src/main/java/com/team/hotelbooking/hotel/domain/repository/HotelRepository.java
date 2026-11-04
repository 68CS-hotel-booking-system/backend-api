package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface HotelRepository {

  Hotel add(Hotel hotel);

  Optional<Hotel> findById(UUID hotelId);

  Optional<Hotel> findByIdAndOwnerId(UUID hotelId, UUID ownerId);

  Optional<Hotel> findByIdAndStatus(UUID hotelId, Hotel.Status status);

  PageResult<Hotel> findAll(PageQuery pagination);

  PageResult<Hotel> findByOwnerId(UUID ownerId, PageQuery pagination);
}
