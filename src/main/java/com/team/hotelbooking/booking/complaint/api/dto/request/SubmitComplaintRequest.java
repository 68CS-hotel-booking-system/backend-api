package com.team.hotelbooking.booking.complaint.api.dto.request;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// category nhận chuỗi để giá trị sai trả 400 với message tiếng Việt thay vì lỗi đọc JSON chung.
public record SubmitComplaintRequest(
    @Schema(
            description =
                "Loại khiếu nại: ROOM_CONDITION (phòng không đúng mô tả, hư hỏng, vệ sinh);"
                    + " SERVICE_QUALITY (chất lượng dịch vụ); STAFF_BEHAVIOR (thái độ nhân viên);"
                    + " BILLING (sai tiền, trừ tiền hai lần, đã thanh toán nhưng đơn chưa xác nhận"
                    + " hoặc đã hết hạn); CANCELLATION_REFUND (hủy, hoàn tiền);"
                    + " OFF_PLATFORM_PAYMENT (bị yêu cầu thanh toán ngoài hệ thống, PV-12);"
                    + " OTHER (khác, mô tả tối thiểu 20 ký tự).",
            example = "ROOM_CONDITION")
        @NotNull(message = "Loại khiếu nại không được để trống.")
        @Pattern(
            regexp =
                "ROOM_CONDITION|SERVICE_QUALITY|STAFF_BEHAVIOR|BILLING|CANCELLATION_REFUND"
                    + "|OFF_PLATFORM_PAYMENT|OTHER",
            message =
                "Loại khiếu nại không hợp lệ. Chọn một trong: ROOM_CONDITION, SERVICE_QUALITY,"
                    + " STAFF_BEHAVIOR, BILLING, CANCELLATION_REFUND, OFF_PLATFORM_PAYMENT, OTHER.")
        String category,
    @Schema(description = "Nội dung khiếu nại, 1–2000 ký tự", example = "Phòng có mùi ẩm mốc.")
        @NotBlank(message = "Nội dung khiếu nại không được để trống.")
        @Size(max = 2000, message = "Nội dung khiếu nại tối đa 2000 ký tự.")
        String content) {

  public Complaint.Category categoryValue() {
    return Complaint.Category.valueOf(category);
  }
}
