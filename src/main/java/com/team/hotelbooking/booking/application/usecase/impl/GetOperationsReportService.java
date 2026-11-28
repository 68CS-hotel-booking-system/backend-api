package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.GetOperationsReportQuery;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.OperationsReportPort;
import com.team.hotelbooking.booking.application.result.OperationsReportResult;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.MonthRow;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.ScopeHotel;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.Totals;
import com.team.hotelbooking.booking.application.usecase.GetOperationsReportUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetOperationsReportService implements GetOperationsReportUseCase {

  private static final long MAX_RANGE_DAYS = 366L;

  private final BookingIdentityPort accounts;
  private final OperationsReportPort reports;

  @Override
  public OperationsReportResult execute(GetOperationsReportQuery query) {
    if (query == null || query.actorId() == null) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    var actor =
        accounts
            .findById(query.actorId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));
    boolean admin = actor.roles().contains("ADMIN");
    boolean owner = actor.roles().contains("HOTEL_OWNER");
    if (!actor.active() || !actor.emailVerified() || (!admin && !owner)) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    LocalDate from = query.from();
    LocalDate to = query.to();
    if (from == null
        || to == null
        || from.isAfter(to)
        || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
      throw new BookingException(
          BookingErrorCode.INVALID_DATE_RANGE,
          "Khoảng ngày báo cáo không hợp lệ (bắt buộc từ ngày <= đến ngày và tối đa 366 ngày).");
    }

    List<ScopeHotel> scopeHotels = reports.findScopeHotels(admin ? null : actor.id());
    List<ScopeHotel> targetHotels;
    if (query.hotelId() != null) {
      ScopeHotel matched =
          scopeHotels.stream()
              .filter(h -> h.hotelId().equals(query.hotelId()))
              .findFirst()
              .orElseThrow(() -> new BookingException(BookingErrorCode.HOTEL_NOT_FOUND));
      targetHotels = List.of(matched);
    } else {
      targetHotels = scopeHotels;
    }

    if (targetHotels.isEmpty()) {
      return emptyReport(from, to, query.hotelId(), scopeHotels);
    }

    return reports.loadReport(from, to, query.hotelId(), scopeHotels, targetHotels);
  }

  private static OperationsReportResult emptyReport(
      LocalDate from, LocalDate to, java.util.UUID hotelId, List<ScopeHotel> scopeHotels) {
    List<MonthRow> months = new ArrayList<>();
    YearMonth cursor = YearMonth.from(from);
    YearMonth end = YearMonth.from(to);
    while (!cursor.isAfter(end)) {
      months.add(new MonthRow(cursor.toString(), 0L, 0L, 0L, null, null, null, 0L, 0L, 0L, 0L));
      cursor = cursor.plusMonths(1);
    }
    return new OperationsReportResult(
        from,
        to,
        hotelId,
        "VND",
        new Totals(0L, 0L, 0L, null, null, null, 0L, 0L, 0L, 0L),
        months,
        List.of(),
        scopeHotels);
  }
}
