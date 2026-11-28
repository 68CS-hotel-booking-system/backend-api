package com.team.hotelbooking.pricing.voucher.api.dto.request;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateVoucherRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 100) String name,
    @NotBlank String discountType,
    @NotNull @Digits(integer = 13, fraction = 0) @Min(1) java.math.BigDecimal discountValue,
    @Digits(integer = 13, fraction = 0) @Min(1) java.math.BigDecimal maxDiscountAmount,
    @NotNull @Digits(integer = 13, fraction = 0) @Min(0) java.math.BigDecimal minOrderAmount,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotNull @Digits(integer = 13, fraction = 0) @Min(1) @Max(1000000)
        java.math.BigDecimal usageLimit) {}
