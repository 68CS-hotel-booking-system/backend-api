package com.team.hotelbooking.hotel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Lý do bắt buộc khi quản trị hệ thống khóa bán khách sạn.")
public record LockHotelRequest(
    @Schema(
            description = "Lý do khóa bán, 1–500 ký tự.",
            example = "Vi phạm quy định giao dịch ngoài hệ thống")
        @NotBlank(message = "Phải nhập lý do khóa bán.")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason) {}
