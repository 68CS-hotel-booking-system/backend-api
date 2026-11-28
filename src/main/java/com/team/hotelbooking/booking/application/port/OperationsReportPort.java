package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.application.result.OperationsReportResult;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.ScopeHotel;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OperationsReportPort {

  List<ScopeHotel> findScopeHotels(UUID ownerId);

  OperationsReportResult loadReport(
      LocalDate from,
      LocalDate to,
      UUID filterHotelId,
      List<ScopeHotel> scopeHotels,
      List<ScopeHotel> targetHotels);
}
