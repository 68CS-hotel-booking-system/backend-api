package com.team.hotelbooking.hotel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Lý do bắt buộc khi quản trị hệ thống gỡ khóa bán khách sạn.")
public record UnlockHotelRequest(
    @Schema(description = "Lý do gỡ khóa, 1–500 ký tự.", example = "Đã xử lý xong vi phạm")
        @NotBlank(message = "Phải nhập lý do gỡ khóa bán.")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason) {}
