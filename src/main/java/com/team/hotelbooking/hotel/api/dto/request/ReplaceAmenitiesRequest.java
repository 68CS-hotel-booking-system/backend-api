package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Thay toàn bộ danh sách; mảng rỗng nghĩa là gỡ hết tiện ích.
public record ReplaceAmenitiesRequest(
    @NotNull(message = "Danh sách tiện ích là bắt buộc.")
        List<@NotNull(message = "Mã tiện ích không được để trống.") UUID> amenityIds) {}
