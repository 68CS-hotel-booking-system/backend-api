package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomAmenityRequest(
    @Size(max = 100, message = "Tên tiện ích tối đa 100 ký tự.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên tiện ích không được để trống.")
        String name,
    @Size(max = 2000, message = "Mô tả tiện ích tối đa 2000 ký tự.") String description,
    Boolean attached,
    @NotNull(message = "Phiên bản tiện ích là bắt buộc.")
        @Min(value = 0, message = "Phiên bản phải không âm.")
        Long expectedVersion) {}
