package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomRequest(
    @NotNull(message = "Loại phòng không được để trống.") UUID roomTypeId,
    @NotBlank(message = "Số phòng không được để trống.")
        @Size(max = 20, message = "Số phòng không được vượt quá 20 ký tự.")
        String roomNumber,
    @Size(max = 20, message = "Tầng không được vượt quá 20 ký tự.") String floor,
    @NotNull(message = "Thời điểm bắt đầu phục vụ không được để trống.") Instant operationalFrom,
    Instant operationalUntil) {}
