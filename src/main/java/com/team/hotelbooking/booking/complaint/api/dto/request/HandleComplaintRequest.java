package com.team.hotelbooking.booking.complaint.api.dto.request;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import jakarta.validation.constraints.*;

public record HandleComplaintRequest(
    @NotNull(message = "Chọn trạng thái xử lý.") Complaint.Status status,
    @NotBlank(message = "Nhập phản hồi gửi khách.")
        @Size(max = 2000, message = "Phản hồi tối đa 2000 ký tự.")
        String response,
    @NotNull(message = "Thiếu phiên bản khiếu nại.")
        @PositiveOrZero(message = "Phiên bản phải không âm.")
        Long expectedVersion) {}
