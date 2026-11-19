package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.UpdateRoomReadinessCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Yêu cầu cập nhật trạng thái sẵn sàng buồng phòng (READY / DIRTY)")
public record UpdateRoomReadinessRequest(
    @Schema(
            description = "Trạng thái buồng phòng (READY hoặc DIRTY)",
            example = "READY",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Trạng thái buồng phòng không được để trống.")
        String readiness) {

  public UpdateRoomReadinessCommand toCommand(UUID actorId, UUID hotelId, UUID roomId) {
    return new UpdateRoomReadinessCommand(actorId, hotelId, roomId, readiness);
  }
}
