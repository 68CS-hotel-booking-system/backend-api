package com.team.hotelbooking.pricing.voucher.api.dto.response;

import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;
import java.time.Instant;
import java.util.UUID;

public record VoucherResponse(
    UUID id,
    UUID hotelId,
    String code,
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    Instant startsAt,
    Instant endsAt,
    int usageLimit,
    long occupiedUses,
    boolean enabled,
    String status,
    long version) {
  public static com.team.hotelbooking.shared.web.PageResponse<VoucherResponse> page(
      com.team.hotelbooking.shared.pagination.PageResult<VoucherResult> result) {
    return com.team.hotelbooking.shared.web.PageResponse.from(result.map(VoucherResponse::from));
  }

  public static VoucherResponse from(VoucherResult v) {
    return new VoucherResponse(
        v.id(),
        v.hotelId(),
        v.code(),
        v.name(),
        v.discountType(),
        v.discountValue(),
        v.maxDiscountAmount(),
        v.minOrderAmount(),
        v.startsAt(),
        v.endsAt(),
        v.usageLimit(),
        v.occupiedUses(),
        v.enabled(),
        v.status(),
        v.version());
  }
}
