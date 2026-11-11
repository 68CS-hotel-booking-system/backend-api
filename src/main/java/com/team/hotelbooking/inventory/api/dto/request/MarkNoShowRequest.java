package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Yêu cầu đánh dấu khách không đến nhận phòng (NO_SHOW)")
public record MarkNoShowRequest(
    @Schema(
            description = "Lý do đánh dấu khách không đến",
            example = "Đã quá hạn nhận phòng và không liên hệ được với khách")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason) {

  public MarkNoShowCommand toCommand(UUID actorId, UUID hotelId, UUID bookingId) {
    return new MarkNoShowCommand(actorId, hotelId, bookingId, reason);
  }
}
