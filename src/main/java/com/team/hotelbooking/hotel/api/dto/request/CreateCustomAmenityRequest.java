package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomAmenityRequest(
    @NotBlank(message = "Tên tiện ích là bắt buộc.")
        @Size(max = 100, message = "Tên tiện ích tối đa 100 ký tự.")
        String name,
    @Size(max = 2000, message = "Mô tả tiện ích tối đa 2000 ký tự.") String description) {}
