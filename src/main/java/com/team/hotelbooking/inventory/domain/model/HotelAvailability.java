package com.team.hotelbooking.inventory.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HotelAvailability(
    UUID hotelId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    int totalNights,
    List<RoomTypeAvailability> roomTypes) {}
