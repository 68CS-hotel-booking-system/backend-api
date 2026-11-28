package com.team.hotelbooking.pricing.voucher.api.dto.request;

import jakarta.validation.constraints.*;

public record SetVoucherEnabledRequest(
    @NotNull Boolean enabled, @NotNull @Min(0) Long expectedVersion) {}
