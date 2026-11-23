package com.team.hotelbooking.booking.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelHotelBookingRequest(
    @NotBlank(message = "Lý do hủy là bắt buộc.")
        @Size(max = 255, message = "Lý do hủy tối đa 255 ký tự.")
        String reason) {}
