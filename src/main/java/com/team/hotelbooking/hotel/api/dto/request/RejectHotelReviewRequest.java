package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RejectHotelReviewRequest(
    @NotBlank String reason, @NotNull @Min(0) Long expectedVersion) {}
