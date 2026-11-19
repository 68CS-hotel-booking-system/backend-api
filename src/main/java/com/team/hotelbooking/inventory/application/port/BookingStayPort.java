package com.team.hotelbooking.inventory.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingStayPort {

  enum StayStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    NO_SHOW
  }

  record BookingItemStayInfo(
      UUID bookingItemId,
      UUID roomTypeId,
      int quantity,
      LocalDate checkInDate,
      LocalDate checkOutDate) {}

  record BookingStayInfo(
      UUID bookingId,
      UUID hotelId,
      StayStatus status,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Instant noShowDeadlineAt,
      Instant lateArrivalExpectedAt,
      String lateArrivalNote,
      List<BookingItemStayInfo> items) {}

  Optional<BookingStayInfo> findBookingForStayOperation(UUID hotelId, UUID bookingId);

  BookingStayInfo recordLateArrival(
      UUID hotelId,
      UUID bookingId,
      Instant expectedArrivalAt,
      String note,
      UUID actorUserId,
      Instant recordedAt);

  BookingStayInfo markCheckedIn(
      UUID hotelId, UUID bookingId, UUID actorUserId, Instant checkedInAt);

  BookingStayInfo markCheckedOut(
      UUID hotelId, UUID bookingId, UUID actorUserId, Instant checkedOutAt);

  BookingStayInfo markNoShow(
      UUID hotelId, UUID bookingId, String reason, UUID actorUserId, Instant markedAt);
}
