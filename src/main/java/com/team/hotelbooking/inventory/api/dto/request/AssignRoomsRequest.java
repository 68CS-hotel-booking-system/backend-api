package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand.RoomAssignmentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Yêu cầu phân công hoặc đổi phòng vật lý cho đơn đặt phòng")
public record AssignRoomsRequest(
    @Schema(
            description = "Danh sách phân công phòng vật lý theo từng hạng mục đặt phòng",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Danh sách phân công phòng không được để trống.")
        List<
                @Valid @NotNull(message = "Hạng mục phân công phòng không được để trống.")
                RoomAssignmentItemRequest>
            assignments,
    @Schema(
            description = "Lý do đổi phòng vật lý (bắt buộc khi thay thế phân công hiện có)",
            example = "Khách yêu cầu đổi sang phòng tầng cao hơn")
        @Size(max = 255, message = "Lý do đổi phòng không được vượt quá 255 ký tự.")
        String reason) {

  public record RoomAssignmentItemRequest(
      @Schema(description = "ID hạng mục đặt phòng", requiredMode = Schema.RequiredMode.REQUIRED)
          @NotNull(message = "Mã hạng mục đặt phòng không được để trống.")
          UUID bookingItemId,
      @Schema(
              description = "Danh sách ID phòng vật lý được gán cho hạng mục này",
              requiredMode = Schema.RequiredMode.REQUIRED)
          @NotEmpty(message = "Danh sách phòng vật lý của hạng mục không được để trống.")
          List<@NotNull(message = "Mã phòng vật lý không được để trống.") UUID> roomIds) {}

  public AssignRoomsCommand toCommand(UUID actorId, UUID hotelId, UUID bookingId) {
    List<RoomAssignmentItem> items =
        assignments == null
            ? List.of()
            : assignments.stream()
                .map(i -> new RoomAssignmentItem(i.bookingItemId(), i.roomIds()))
                .toList();
    return new AssignRoomsCommand(actorId, hotelId, bookingId, items, reason);
  }
}
