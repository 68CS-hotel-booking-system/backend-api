package com.team.hotelbooking.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Nhu cầu giữ phòng của Booking; Inventory cung cấp triển khai để tránh phụ thuộc vòng. */
public interface BookingRoomHoldGateway {

  void hold(HoldRequest request);

  record HoldRequest(
      UUID bookingId,
      UUID hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      Instant expiresAt,
      List<HoldItem> items) {}

  record HoldItem(UUID bookingItemId, UUID roomTypeId, int quantity) {}
}
