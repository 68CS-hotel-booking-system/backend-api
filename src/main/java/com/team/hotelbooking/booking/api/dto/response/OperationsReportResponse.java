package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.OperationsReportResult;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OperationsReportResponse(
    LocalDate from,
    LocalDate to,
    UUID hotelId,
    String currency,
    TotalsResponse totals,
    List<MonthRowResponse> months,
    List<HotelRowResponse> hotels,
    List<ScopeHotelResponse> scopeHotels) {

  public record TotalsResponse(
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

  public record MonthRowResponse(
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

  public record HotelRowResponse(
      UUID hotelId,
      String hotelName,
      long availableRoomNights,
      long soldRoomNights,
      long roomRevenue,
      Double occupancyRate,
      Long adr,
      Long revpar,
      long completedRevenue) {}

  public record ScopeHotelResponse(UUID hotelId, String hotelName) {}

  public static OperationsReportResponse from(OperationsReportResult result) {
    var t = result.totals();
    return new OperationsReportResponse(
        result.from(),
        result.to(),
        result.hotelId(),
        result.currency(),
        new TotalsResponse(
            t.availableRoomNights(),
            t.soldRoomNights(),
            t.roomRevenue(),
            t.occupancyRate(),
            t.adr(),
            t.revpar(),
            t.completedRevenue(),
            t.newBookings(),
            t.cancelledBookings(),
            t.noShowBookings()),
        result.months().stream()
            .map(
                m ->
                    new MonthRowResponse(
                        m.month(),
                        m.availableRoomNights(),
                        m.soldRoomNights(),
                        m.roomRevenue(),
                        m.occupancyRate(),
                        m.adr(),
                        m.revpar(),
                        m.completedRevenue(),
                        m.newBookings(),
                        m.cancelledBookings(),
                        m.noShowBookings()))
            .toList(),
        result.hotels().stream()
            .map(
                h ->
                    new HotelRowResponse(
                        h.hotelId(),
                        h.hotelName(),
                        h.availableRoomNights(),
                        h.soldRoomNights(),
                        h.roomRevenue(),
                        h.occupancyRate(),
                        h.adr(),
                        h.revpar(),
                        h.completedRevenue()))
            .toList(),
        result.scopeHotels().stream()
            .map(h -> new ScopeHotelResponse(h.hotelId(), h.hotelName()))
            .toList());
  }
}
