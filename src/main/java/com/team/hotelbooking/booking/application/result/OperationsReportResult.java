package com.team.hotelbooking.booking.application.result;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OperationsReportResult(
    LocalDate from,
    LocalDate to,
    UUID hotelId,
    String currency,
    Totals totals,
    List<MonthRow> months,
    List<HotelRow> hotels,
    List<ScopeHotel> scopeHotels) {

  public OperationsReportResult {
    months = List.copyOf(months);
    hotels = List.copyOf(hotels);
    scopeHotels = List.copyOf(scopeHotels);
  }

  public record Totals(
      long availableRoomNights,
      long soldRoomNights,
      long roomRevenue,
      Double occupancyRate,
      Long adr,
      Long revpar,
      long completedRevenue,
      long newBookings,
      long cancelledBookings,
      long noShowBookings) {}

  public record MonthRow(
      String month,
      long availableRoomNights,
      long soldRoomNights,
      long roomRevenue,
      Double occupancyRate,
      Long adr,
      Long revpar,
      long completedRevenue,
      long newBookings,
      long cancelledBookings,
      long noShowBookings) {}

  public record HotelRow(
      UUID hotelId,
      String hotelName,
      long availableRoomNights,
      long soldRoomNights,
      long roomRevenue,
      Double occupancyRate,
      Long adr,
      Long revpar,
      long completedRevenue) {}

  public record ScopeHotel(UUID hotelId, String hotelName) {}

  public static Double occupancyRate(long sold, long available) {
    if (available <= 0L) {
      return null;
    }
    return Math.round(((double) sold / (double) available) * 10000.0) / 10000.0;
  }

  public static Long adr(long roomRevenue, long sold) {
    return sold <= 0L ? null : roomRevenue / sold;
  }

  public static Long revpar(long roomRevenue, long available) {
    return available <= 0L ? null : roomRevenue / available;
  }
}
