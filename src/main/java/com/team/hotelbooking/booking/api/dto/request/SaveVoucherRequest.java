package com.team.hotelbooking.booking.api.dto.request;

import com.team.hotelbooking.booking.application.command.SaveVoucherCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaveVoucherRequest(
    @NotBlank(message = "Tên voucher không được để trống.")
        @Size(max = 150, message = "Tên voucher không được vượt quá 150 ký tự.")
        String name,
    @NotBlank(message = "Loại giảm giá không được để trống.") String discountType,
    BigDecimal percentageOff,
    Long fixedAmount,
    Long maxDiscountAmount,
    Long minBookingAmount,
    Integer usageLimit,
    Integer perUserLimit,
    @NotNull(message = "Thời điểm bắt đầu không được để trống.") Instant startsAt,
    @NotNull(message = "Thời điểm kết thúc không được để trống.") Instant endsAt,
    String status,
    String fundingSource) {

  public SaveVoucherCommand toCommand(UUID actorId, UUID hotelId, String code, boolean create) {
    return new SaveVoucherCommand(
        actorId,
        hotelId,
        code,
        create,
        name,
        discountType,
        percentageOff,
        fixedAmount,
        maxDiscountAmount,
        minBookingAmount,
        usageLimit,
        perUserLimit,
        startsAt,
        endsAt,
        status,
        fundingSource);
  }
}
