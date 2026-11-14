package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.HotelPublishReadinessResponse;
import com.team.hotelbooking.hotel.application.command.GetHotelPublishReadinessQuery;
import com.team.hotelbooking.hotel.application.usecase.GetHotelPublishReadinessUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HotelPublishReadinessController {
  private final GetHotelPublishReadinessUseCase readiness;

  @GetMapping("/api/v1/management/hotels/{hotelId}/publish-readiness")
  @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<HotelPublishReadinessResponse>> get(
      @PathVariable UUID hotelId, Authentication authentication) {
    var result =
        readiness.execute(
            new GetHotelPublishReadinessQuery(
                CurrentActor.userId(authentication),
                CurrentActor.isAdmin(authentication),
                hotelId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy điều kiện gửi hồ sơ thành công.", HotelPublishReadinessResponse.from(result)));
  }
}
