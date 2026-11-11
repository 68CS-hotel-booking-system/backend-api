package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.CreateRoomBlockCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record CreateRoomBlockRequest(
    @NotNull(message = "Mã phòng vật lý không được để trống.") UUID roomId,
    @NotNull(message = "Ngày bắt đầu chặn không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
    @NotNull(message = "Ngày kết thúc chặn không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,
    @NotBlank(message = "Lý do chặn phòng không được để trống.")
        @Size(max = 255, message = "Lý do chặn phòng tối đa 255 ký tự.")
        String reason) {

  public CreateRoomBlockCommand toCommand(UUID actorId, UUID hotelId) {
    return new CreateRoomBlockCommand(actorId, hotelId, roomId, startDate, endDate, reason);
  }
}
