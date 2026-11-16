package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.request.QuoteVoucherRequest;
import com.team.hotelbooking.booking.api.dto.response.BookingDetailResponse;
import com.team.hotelbooking.booking.api.dto.response.VoucherQuoteResponse;
import com.team.hotelbooking.booking.application.usecase.CancelBookingUseCase;
import com.team.hotelbooking.booking.application.usecase.QuoteVoucherUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingCheckoutController {

  private final QuoteVoucherUseCase quoteVoucherUseCase;
  private final CancelBookingUseCase cancelBookingUseCase;

  @PostMapping("/voucher-quote")
  public ResponseEntity<ApiResponse<VoucherQuoteResponse>> quoteVoucher(
      @Valid @RequestBody QuoteVoucherRequest request, Principal principal) {
    var result =
        quoteVoucherUseCase.execute(
            UUID.fromString(principal.getName()), request.voucherCode(), request.toCriteria());
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Báo giá voucher thành công.", VoucherQuoteResponse.from(result)));
  }

  @PostMapping("/{bookingId}/cancel")
  public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
      @PathVariable("bookingId") UUID bookingId, Principal principal) {
    var result = cancelBookingUseCase.execute(UUID.fromString(principal.getName()), bookingId);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Đã đóng đơn đặt phòng.", BookingDetailResponse.from(result)));
  }
}
