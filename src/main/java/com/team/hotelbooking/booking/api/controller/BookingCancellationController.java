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
  private final com.team.hotelbooking.booking.application.usecase.GetCancellationQuoteUseCase quote;

  public record CancellationQuoteResponse(
      boolean canCancel,
      String message,
      long expectedVersion,
      long refundAmount,
      String currency,
      java.time.Instant cutoff) {
    static CancellationQuoteResponse from(
        com.team.hotelbooking.booking.application.result.CancellationQuoteResult r) {
      return new CancellationQuoteResponse(
          r.canCancel(),
          r.message(),
          r.expectedVersion(),
          r.refundAmount(),
          r.currency(),
          r.cutoff());
    }
  }

  public record CancelRequest(
      @jakarta.validation.constraints.Min(0) Long expectedVersion,
      @jakarta.validation.constraints.Min(0) Long expectedRefundAmount,
      @jakarta.validation.constraints.Size(max = 1000) String reason) {}

  @GetMapping("/api/v1/bookings/{bookingId}/cancellation")
  public ResponseEntity<ApiResponse<CancellationQuoteResponse>> quote(
      @PathVariable("bookingId") UUID bookingId, Principal principal) {
    var result =
        quote.quote(new CancelBookingCommand(UUID.fromString(principal.getName()), bookingId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Chính sách hủy đơn.", CancellationQuoteResponse.from(result)));
  }

  @PostMapping("/api/v1/bookings/{bookingId}/cancel")
  public ResponseEntity<ApiResponse<CancelBookingResponse>> cancel(
      @PathVariable("bookingId") UUID bookingId,
      @jakarta.validation.Valid @RequestBody(required = false) CancelRequest body,
      Principal principal) {
    var result =
        cancel.execute(
            new CancelBookingCommand(
                UUID.fromString(principal.getName()),
                bookingId,
                body == null ? null : body.expectedVersion(),
                body == null ? null : body.expectedRefundAmount(),
                body == null ? null : body.reason()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đã xử lý hủy đơn đặt phòng.", CancelBookingResponse.from(result)));
  }
}
