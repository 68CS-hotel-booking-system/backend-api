package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.response.FinanceDashboardResponse;
import com.team.hotelbooking.payment.application.command.GetFinanceDashboardQuery;
import com.team.hotelbooking.payment.application.usecase.GetFinanceDashboardUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FinanceDashboardController {
  private final GetFinanceDashboardUseCase useCase;

  @GetMapping("/api/v1/finance/dashboard")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<FinanceDashboardResponse>> getDashboard(
      Authentication authentication) {
    var result =
        useCase.execute(
            new GetFinanceDashboardQuery(
                CurrentActor.userId(authentication), CurrentActor.isAdmin(authentication)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy báo cáo doanh thu thành công.", FinanceDashboardResponse.from(result)));
  }
}
