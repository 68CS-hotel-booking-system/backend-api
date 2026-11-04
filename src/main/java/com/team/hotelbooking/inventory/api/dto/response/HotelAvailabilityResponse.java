package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HotelAvailabilityResponse(
    UUID hotelId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    int totalNights,
    List<RoomTypeAvailabilityDto> roomTypes) {

  public static HotelAvailabilityResponse from(CheckAvailabilityResult result) {
    List<RoomTypeAvailabilityDto> dtos =
        result.roomTypes().stream().map(RoomTypeAvailabilityDto::from).toList();

    return new HotelAvailabilityResponse(
        result.hotelId(), result.checkInDate(), result.checkOutDate(), result.totalNights(), dtos);
  }
}
