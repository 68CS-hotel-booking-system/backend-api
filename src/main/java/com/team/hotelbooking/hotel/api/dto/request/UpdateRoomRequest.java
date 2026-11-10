package com.team.hotelbooking.hotel.api.dto.request;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Yêu cầu cập nhật thông tin phòng vật lý hoặc ngừng sử dụng")
public record UpdateRoomRequest(
    @Schema(description = "Số phòng mới", example = "P.302")
        @Size(min = 1, max = 50, message = "Số phòng phải từ 1 đến 50 ký tự.")
        String roomNumber,
    @Schema(description = "Tầng", example = "3") Short floor,
    @Schema(
            description = "Trạng thái buồng phòng (ready, cleaning, maintenance)",
            example = "ready",
            allowableValues = {"ready", "cleaning", "maintenance"})
        String status,
    @Schema(
            description = "Trạng thái phòng hoạt động (true: sử dụng, false: ngừng sử dụng)",
            example = "true")
        Boolean active) {

  public UpdateRoomCommand toCommand(UUID actorUserId, boolean admin, UUID hotelId, UUID roomId) {
    return new UpdateRoomCommand(
        actorUserId, admin, hotelId, roomId, roomNumber, floor, status, active);
  }
}
