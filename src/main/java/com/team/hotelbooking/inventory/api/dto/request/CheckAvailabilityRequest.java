package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record CheckAvailabilityRequest(
    @Parameter(description = "Ngày nhận phòng (YYYY-MM-DD)", required = true)
        @NotNull(message = "Ngày nhận phòng không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkInDate,
    @Parameter(description = "Ngày trả phòng (YYYY-MM-DD)", required = true)
        @NotNull(message = "Ngày trả phòng không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkOutDate,
    @Parameter(description = "Số lượng người lớn (mặc định: 1)")
        @Min(value = 1, message = "Số lượng người lớn tối thiểu là 1.")
        Integer adults,
    @Parameter(description = "Số lượng trẻ em (mặc định: 0)")
        @Min(value = 0, message = "Số lượng trẻ em không được âm.")
        Integer children,
    @Parameter(description = "Số lượng phòng cần đặt (mặc định: 1)")
        @Min(value = 1, message = "Số lượng phòng tối thiểu là 1.")
        Integer quantity) {

  public CheckAvailabilityRequest {
    if (adults == null) {
      adults = 1;
    }
    if (children == null) {
      children = 0;
    }
    if (quantity == null) {
      quantity = 1;
    }
  }

  public CheckAvailabilityQuery toQuery(UUID hotelId) {
    return new CheckAvailabilityQuery(
        hotelId, checkInDate, checkOutDate, adults, children, quantity);
  }
}
