package com.team.hotelbooking.pricing.api.dto.request;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record GetRoomRatesRequest(
    @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)", required = true)
        @NotNull(message = "Ngày bắt đầu không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
    @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)", required = true)
        @NotNull(message = "Ngày kết thúc không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate) {

  public GetRoomRatesQuery toQuery(UUID actorUserId, boolean admin, UUID hotelId, UUID roomTypeId) {
    return new GetRoomRatesQuery(actorUserId, admin, hotelId, roomTypeId, startDate, endDate);
  }
}
