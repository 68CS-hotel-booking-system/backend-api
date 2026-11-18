package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.request.SaveVoucherRequest;
import com.team.hotelbooking.booking.api.dto.response.SaveVoucherResponse;
import com.team.hotelbooking.booking.application.usecase.SaveVoucherUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VoucherManagementController {

  private final SaveVoucherUseCase saveVoucherUseCase;

  @PostMapping({
    "/api/v1/admin/vouchers/{code}",
    "/api/v1/management/hotels/{hotelId}/vouchers/{code}"
  })
  public ResponseEntity<ApiResponse<SaveVoucherResponse>> create(
      @PathVariable(name = "hotelId", required = false) UUID hotelId,
      @PathVariable("code") String code,
      @Valid @RequestBody SaveVoucherRequest request,
      Principal principal) {
    var result =
        saveVoucherUseCase.execute(
            request.toCommand(UUID.fromString(principal.getName()), hotelId, code, true));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo voucher thành công.", SaveVoucherResponse.from(result)));
  }

  @PutMapping({
    "/api/v1/admin/vouchers/{code}",
    "/api/v1/management/hotels/{hotelId}/vouchers/{code}"
  })
  public ResponseEntity<ApiResponse<SaveVoucherResponse>> update(
      @PathVariable(name = "hotelId", required = false) UUID hotelId,
      @PathVariable("code") String code,
      @Valid @RequestBody SaveVoucherRequest request,
      Principal principal) {
    var result =
        saveVoucherUseCase.execute(
            request.toCommand(UUID.fromString(principal.getName()), hotelId, code, false));
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật voucher thành công.", SaveVoucherResponse.from(result)));
  }
}
