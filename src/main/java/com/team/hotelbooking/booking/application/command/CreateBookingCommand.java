package com.team.hotelbooking.booking.application.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(
    UUID customerId,
    UUID hotelId,
    LocalDate checkIn,
    LocalDate checkOut,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    ContactInput booker,
    ContactInput guest,
    List<BookingItemInput> items,
    Long quotedTotalAmount) {

  public record ContactInput(String fullName, String email, String phone) {}

  public record BookingItemInput(UUID roomTypeId, int quantity, int adults, int children) {}
}
