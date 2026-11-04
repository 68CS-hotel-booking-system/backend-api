package com.team.hotelbooking.inventory.application.result;

import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckAvailabilityResult(
    UUID hotelId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    int totalNights,
    List<RoomTypeAvailabilityResult> roomTypes) {

  public static CheckAvailabilityResult from(HotelAvailability domain) {
    List<RoomTypeAvailabilityResult> roomTypes =
        domain.roomTypes().stream().map(RoomTypeAvailabilityResult::from).toList();

    return new CheckAvailabilityResult(
        domain.hotelId(),
        domain.checkInDate(),
        domain.checkOutDate(),
        domain.totalNights(),
        roomTypes);
  }
}
