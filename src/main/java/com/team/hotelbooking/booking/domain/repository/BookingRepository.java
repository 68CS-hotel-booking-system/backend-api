package com.team.hotelbooking.booking.domain.repository;

import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

  Booking add(Booking booking);

  Optional<Booking> findById(UUID bookingId);

  Optional<Booking> lockById(UUID bookingId);

  void closePending(UUID bookingId, Booking.Status status, UUID actorId, Instant now);

  void cancelConfirmed(
      UUID bookingId, UUID actorId, String reason, boolean cancelledByHotel, Instant now);

  Optional<Booking> findByIdAndCustomerId(UUID bookingId, UUID customerId);

  Optional<Booking> findByIdAndHotelId(UUID bookingId, UUID hotelId);

  PageResult<Booking> findByCustomerId(
      UUID customerId, Booking.Status status, PageQuery pagination);

  PageResult<Booking> findByHotelId(UUID hotelId, Booking.Status status, PageQuery pagination);

  boolean updateStatus(UUID bookingId, Booking.Status status, Instant updatedAt);
}
