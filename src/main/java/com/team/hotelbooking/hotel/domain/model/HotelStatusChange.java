package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;

// Một lần đổi trạng thái bán của khách sạn; chỉ thêm mới, không sửa/xóa (HT-08).
public record HotelStatusChange(
    UUID id,
    UUID hotelId,
    Hotel.Status fromStatus,
    Hotel.Status toStatus,
    String reason,
    UUID changedByUserId,
    Instant changedAt) {

  public static final int MAX_REASON_LENGTH = 500;

  public static HotelStatusChange of(
      UUID id, Hotel before, Hotel after, String reason, boolean reasonRequired, UUID actorId) {
    if (id == null || before == null || after == null || actorId == null) {
      throw invalid("Định danh, khách sạn và người thực hiện đổi trạng thái là bắt buộc.");
    }

    String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
    if (reasonRequired && normalizedReason == null) {
      throw invalid("Phải nhập lý do khóa hoặc gỡ khóa bán.");
    }
    if (normalizedReason != null && normalizedReason.length() > MAX_REASON_LENGTH) {
      throw invalid("Lý do không được vượt quá " + MAX_REASON_LENGTH + " ký tự.");
    }

    return new HotelStatusChange(
        id,
        after.id(),
        before.status(),
        after.status(),
        normalizedReason,
        actorId,
        after.updatedAt());
  }

  private static HotelException invalid(String message) {
    return new HotelException(HotelErrorCode.VALIDATION_FAILED, message);
  }
}
