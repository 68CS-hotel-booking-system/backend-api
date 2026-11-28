package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateHotelMemberStatusRequest(
    @NotBlank(message = "Trạng thái phân công là bắt buộc.")
        @Pattern(
            regexp = "ACTIVE|REVOKED",
            message = "Trạng thái phân công chỉ chấp nhận ACTIVE hoặc REVOKED.")
        String status,
    @NotNull(message = "Phiên bản hiện tại là bắt buộc.")
        @PositiveOrZero(message = "Phiên bản không hợp lệ.")
        Long expectedVersion) {}
