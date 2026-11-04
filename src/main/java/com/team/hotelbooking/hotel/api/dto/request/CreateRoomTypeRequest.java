package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomTypeRequest(
    @NotBlank(message = "Tên loại phòng không được để trống.")
        @Size(max = 100, message = "Tên loại phòng không được vượt quá 100 ký tự.")
        String name,
    String description,
    @NotNull(message = "Số người lớn tối đa không được để trống.")
        @Min(value = 1, message = "Số người lớn tối đa phải từ 1.")
        Short maxAdults,
    @NotNull(message = "Số trẻ em tối đa không được để trống.")
        @Min(value = 0, message = "Số trẻ em tối đa không được âm.")
        Short maxChildren) {}
