package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.RoomResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Thông tin chi tiết phòng vật lý")
public record RoomResponse(
    @Schema(description = "ID phòng vật lý", example = "550e8400-e29b-41d4-a716-446655440010")
        UUID id,
    @Schema(description = "ID khách sạn", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID hotelId,
    @Schema(description = "ID loại phòng", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID roomTypeId,
    @Schema(description = "Số phòng", example = "P.302") String roomNumber,
    @Schema(description = "Tầng", example = "3") Short floor,
    @Schema(description = "Trạng thái buồng phòng", example = "ready") String status,
    @Schema(description = "Trạng thái hoạt động", example = "true") boolean active,
    @Schema(description = "Thời điểm khởi tạo") Instant createdAt,
    @Schema(description = "Thời điểm cập nhật gần nhất") Instant updatedAt) {

  public static RoomResponse from(RoomResult result) {
    return new RoomResponse(
        result.id(),
        result.hotelId(),
        result.roomTypeId(),
        result.roomNumber(),
        result.floor(),
        result.status(),
        result.active(),
        result.createdAt(),
        result.updatedAt());
  }
}
