package com.team.hotelbooking.inventory.api.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HotelAvailabilityResponse(
    UUID hotelId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    int totalNights,
    List<RoomTypeAvailabilityDto> roomTypes) {}
