package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.response.CancelBookingResponse;
import com.team.hotelbooking.booking.application.command.CancelBookingCommand;
import com.team.hotelbooking.booking.application.usecase.CancelBookingUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BookingCancellationController {
  private final CancelBookingUseCase cancel;

  @PostMapping("/api/v1/bookings/{bookingId}/cancel")
  public ResponseEntity<ApiResponse<CancelBookingResponse>> cancel(
      @PathVariable("bookingId") UUID bookingId, Principal principal) {
    var result =
        cancel.execute(new CancelBookingCommand(UUID.fromString(principal.getName()), bookingId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đã xử lý hủy đơn đặt phòng.", CancelBookingResponse.from(result)));
  }
}
