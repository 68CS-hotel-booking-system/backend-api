package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.response.OperationsReportResponse;
import com.team.hotelbooking.booking.application.command.GetOperationsReportQuery;
import com.team.hotelbooking.booking.application.usecase.GetOperationsReportUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OperationsReportController {

  private final GetOperationsReportUseCase operationsReportUseCase;

  @GetMapping("/api/v1/management/reports/operations")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<OperationsReportResponse>> getOperationsReport(
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @RequestParam(name = "hotelId", required = false) UUID hotelId,
      Principal principal) {
    var result =
        operationsReportUseCase.execute(
            new GetOperationsReportQuery(UUID.fromString(principal.getName()), hotelId, from, to));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy báo cáo vận hành thành công.", OperationsReportResponse.from(result)));
  }
}
