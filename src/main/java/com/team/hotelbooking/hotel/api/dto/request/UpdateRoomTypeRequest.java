package com.team.hotelbooking.hotel.api.dto.request;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Yêu cầu cập nhật thông tin loại phòng hoặc ngừng bán")
public record UpdateRoomTypeRequest(
    @Schema(description = "Tên loại phòng mới", example = "Phòng Deluxe Hướng Biển")
        @Size(min = 1, max = 100, message = "Tên loại phòng phải từ 1 đến 100 ký tự.")
        String name,
    @Schema(
            description = "Mô tả chi tiết loại phòng",
            example = "Phòng có ban công nhìn ra biển...")
        String description,
    @Schema(description = "Số lượng người lớn tối đa", example = "2")
        @Min(value = 1, message = "Số người lớn tối đa phải ít nhất là 1.")
        Short maxAdults,
    @Schema(description = "Số lượng trẻ em tối đa", example = "1")
        @Min(value = 0, message = "Số trẻ em tối đa không được âm.")
        Short maxChildren,
    @Schema(description = "Sức chứa tối đa của phòng", example = "3")
        @Min(value = 1, message = "Sức chứa tối đa phải ít nhất là 1.")
        Short maxOccupancy,
    @Schema(description = "Giá cơ bản mỗi đêm (VND)", example = "1500000")
        @Min(value = 0, message = "Giá cơ bản phải lớn hơn hoặc bằng 0.")
        Long basePrice,
    @Schema(description = "Trạng thái mở bán (true: mở bán, false: ngừng bán)", example = "true")
        Boolean active) {

  public UpdateRoomTypeCommand toCommand(
      UUID actorUserId, boolean admin, UUID hotelId, UUID roomTypeId) {
    return new UpdateRoomTypeCommand(
        actorUserId,
        admin,
        hotelId,
        roomTypeId,
        name,
        description,
        maxAdults,
        maxChildren,
        maxOccupancy,
        basePrice,
        active);
  }
}
