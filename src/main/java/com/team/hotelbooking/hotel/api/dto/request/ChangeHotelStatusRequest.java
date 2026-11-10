package com.team.hotelbooking.hotel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
    description =
        "Lý do tùy chọn khi công khai, tạm ngừng hoặc mở bán lại khách sạn; được lưu vào lịch sử trạng thái.")
public record ChangeHotelStatusRequest(
    @Schema(
            description = "Lý do, tối đa 500 ký tự; có thể bỏ trống.",
            example = "Tạm ngừng để sửa chữa hồ bơi")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason) {}
