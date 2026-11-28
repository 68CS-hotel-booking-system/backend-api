package com.team.hotelbooking.pricing.voucher.api.controller;

import com.team.hotelbooking.pricing.voucher.api.dto.request.*;
import com.team.hotelbooking.pricing.voucher.api.dto.response.VoucherResponse;
import com.team.hotelbooking.pricing.voucher.application.command.*;
import com.team.hotelbooking.pricing.voucher.application.usecase.*;
import com.team.hotelbooking.shared.web.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/management/hotels/{hotelId}/vouchers")
@PreAuthorize("hasRole('HOTEL_OWNER') and !hasRole('ADMIN')")
public class VoucherController {
  private final CreateVoucherUseCase create;
  private final ListVouchersUseCase list;
  private final SetVoucherEnabledUseCase toggle;
  private final PaginationSupport pagination;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> list(
      @PathVariable("hotelId") UUID hotel,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal p) {
    var result =
        list.execute(
            new ListVouchersQuery(
                UUID.fromString(p.getName()), hotel, pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Danh sách voucher.", VoucherResponse.page(result)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<VoucherResponse>> create(
      @PathVariable("hotelId") UUID hotel,
      @Valid @RequestBody CreateVoucherRequest r,
      Principal p) {
    var result =
        create.execute(
            new CreateVoucherCommand(
                UUID.fromString(p.getName()),
                hotel,
                r.code(),
                r.name(),
                r.discountType(),
                r.discountValue().longValueExact(),
                r.maxDiscountAmount() == null ? null : r.maxDiscountAmount().longValueExact(),
                r.minOrderAmount().longValueExact(),
                r.startsAt(),
                r.endsAt(),
                r.usageLimit().intValueExact()));
    return ResponseEntity.status(201)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Đã tạo voucher.", VoucherResponse.from(result)));
  }

  @PatchMapping("/{voucherId}")
  public ResponseEntity<ApiResponse<VoucherResponse>> toggle(
      @PathVariable("hotelId") UUID hotel,
      @PathVariable("voucherId") UUID id,
      @Valid @RequestBody SetVoucherEnabledRequest r,
      Principal p) {
    var result =
        toggle.execute(
            new SetVoucherEnabledCommand(
                UUID.fromString(p.getName()), hotel, id, r.enabled(), r.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Đã cập nhật trạng thái voucher.", VoucherResponse.from(result)));
  }
}
