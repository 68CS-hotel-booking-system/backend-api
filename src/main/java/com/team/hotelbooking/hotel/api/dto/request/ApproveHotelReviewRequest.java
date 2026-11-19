package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApproveHotelReviewRequest(@NotNull @Min(0) Long expectedVersion) {}
