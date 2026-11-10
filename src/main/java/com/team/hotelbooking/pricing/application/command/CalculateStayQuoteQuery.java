package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CalculateStayQuoteQuery(
    UUID hotelId,
    LocalDate checkIn,
    LocalDate checkOut,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    List<QuoteItemInput> items) {

  public record QuoteItemInput(UUID roomTypeId, int quantity, int adults, int children) {}
}
