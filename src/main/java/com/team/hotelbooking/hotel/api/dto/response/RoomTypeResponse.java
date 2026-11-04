package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Thông tin chi tiết loại phòng")
public record RoomTypeResponse(
    @Schema(description = "ID loại phòng", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
    @Schema(description = "ID khách sạn", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID hotelId,
    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển") String name,
    @Schema(description = "Mô tả loại phòng", example = "Phòng có ban công nhìn ra biển...")
        String description,
    @Schema(description = "Số người lớn tối đa", example = "2") short maxAdults,
    @Schema(description = "Số trẻ em tối đa", example = "1") short maxChildren,
    @Schema(description = "Sức chứa tối đa", example = "3") short maxOccupancy,
    @Schema(description = "Giá cơ bản mỗi đêm (VND)", example = "1500000") long basePrice,
    @Schema(description = "Trạng thái mở bán", example = "true") boolean active,
    @Schema(description = "Thời điểm khởi tạo") Instant createdAt,
    @Schema(description = "Thời điểm cập nhật gần nhất") Instant updatedAt) {

  public static RoomTypeResponse from(RoomTypeResult result) {
    return new RoomTypeResponse(
        result.id(),
        result.hotelId(),
        result.name(),
        result.description(),
        result.maxAdults(),
        result.maxChildren(),
        result.maxOccupancy(),
        result.basePrice(),
        result.active(),
        result.createdAt(),
        result.updatedAt());
  }
}
