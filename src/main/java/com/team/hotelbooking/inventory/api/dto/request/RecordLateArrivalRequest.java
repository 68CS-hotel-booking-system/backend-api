package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.RecordLateArrivalCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Yêu cầu ghi nhận lịch khách đến nhận phòng muộn")
public record RecordLateArrivalRequest(
    @Schema(
            description = "Thời điểm dự kiến khách đến nhận phòng (ISO-8601 UTC)",
            example = "2026-11-11T03:00:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Thời điểm dự kiến khách đến muộn không được để trống.")
        Instant expectedArrivalAt,
    @Schema(
            description = "Ghi chú thông báo đến muộn của khách",
            example = "Chuyến bay bị trễ, khách báo đến lúc 10h sáng hôm sau")
        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
        String note) {

  public RecordLateArrivalCommand toCommand(UUID actorId, UUID hotelId, UUID bookingId) {
    return new RecordLateArrivalCommand(actorId, hotelId, bookingId, expectedArrivalAt, note);
  }
}
