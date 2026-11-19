package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.ReleaseRoomBlockCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReleaseRoomBlockRequest(
    @NotBlank(message = "Lý do giải phóng lịch chặn không được để trống.")
        @Size(max = 255, message = "Lý do giải phóng tối đa 255 ký tự.")
        String reason) {

  public ReleaseRoomBlockCommand toCommand(UUID actorId, UUID hotelId, UUID blockId) {
    return new ReleaseRoomBlockCommand(actorId, hotelId, blockId, reason);
  }
}
