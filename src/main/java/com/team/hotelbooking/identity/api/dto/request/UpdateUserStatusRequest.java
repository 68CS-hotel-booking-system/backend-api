package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
    @NotBlank
        @Pattern(
            regexp = "active|inactive|pending|banned",
            message = "Trạng thái đích phải là active, inactive, pending hoặc banned.")
        String status,
    @Size(max = 500) String reason,
    @NotNull @PositiveOrZero Long expectedVersion) {}
