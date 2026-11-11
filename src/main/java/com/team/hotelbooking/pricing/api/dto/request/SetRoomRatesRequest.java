package com.team.hotelbooking.pricing.api.dto.request;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Yêu cầu thiết lập giá phòng cho ngày hoặc khoảng ngày")
public record SetRoomRatesRequest(
    @Schema(
            description = "Ngày bắt đầu áp dụng giá (YYYY-MM-DD)",
            example = "2026-11-10",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Ngày bắt đầu không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
    @Schema(
            description =
                "Ngày kết thúc không bao gồm (YYYY-MM-DD). Nếu để trống, giá sẽ được đặt cho đúng ngày startDate.",
            example = "2026-11-15")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDateExclusive,
    @Schema(
            description = "Giá phòng mỗi đêm bằng VND (số nguyên dương)",
            example = "1200000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Giá phòng không được để trống.")
        @Min(value = 1, message = "Giá phòng phải lớn hơn 0.")
        Long amount,
    @Schema(description = "Đơn vị tiền tệ (mặc định VND)", example = "VND") String currency) {

  public SetRoomRatesCommand toCommand(
      UUID actorUserId, boolean admin, UUID hotelId, UUID roomTypeId) {
    return new SetRoomRatesCommand(
        actorUserId,
        admin,
        hotelId,
        roomTypeId,
        startDate,
        endDateExclusive,
        amount,
        currency == null || currency.isBlank() ? "VND" : currency.trim().toUpperCase());
  }
}
