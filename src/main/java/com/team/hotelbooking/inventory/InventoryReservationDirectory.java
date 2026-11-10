package com.team.hotelbooking.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationDirectory {

  /**
   * Giữ đủ tồn kho cho mọi hạng mục và mọi đêm trong [checkInDate, checkOutDate) có khóa chống
   * tranh phòng cuối (IV-05, IV-06, IV-08, IV-13). Gọi lặp với cùng bookingId trả lại kết quả đã
   * giữ trước đó (BK-12).
   */
  HoldOutcome hold(HoldRequest request);

  /**
   * Chuyển các bản ghi giữ chỗ còn hiệu lực (HELD và expiresAt > now) của đơn sang CONFIRMED
   * (IV-06, IV-07). Gọi lặp khi đã CONFIRMED là thao tác an toàn. Nếu giữ chỗ đã hết hạn hoặc đã
   * giải phóng thì ném InventoryException(HOLD_EXPIRED).
   */
  void confirm(UUID bookingId);

  /**
   * Giải phóng tồn kho đang giữ hoặc đã xác nhận đúng một lần khi hủy đơn hoặc hết hạn giữ chỗ
   * (IV-07). Gọi lặp trả về releasedRowCount = 0.
   */
  ReleaseOutcome release(UUID bookingId, ReleaseReason reason);

  /**
   * Giải phóng tồn kho của các đêm chưa sử dụng (stayDate >= fromDate) khi đánh dấu NO_SHOW hợp lệ
   * đúng một lần (IV-11, BK-14). Không sửa tồn lịch sử của các đêm đã qua.
   */
  ReleaseOutcome releaseUnusedNights(UUID bookingId, LocalDate fromDate);

  record HoldItemRequest(UUID bookingItemId, UUID roomTypeId, int quantity) {}

  record HoldRequest(
      UUID bookingId,
      UUID hotelId,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Instant expiresAt,
      List<HoldItemRequest> items) {}

  record HoldOutcome(UUID bookingId, Instant expiresAt, int totalReservedRows, boolean replayed) {}

  enum ReleaseReason {
    EXPIRED,
    CANCELLED,
    NO_SHOW
  }

  record ReleaseOutcome(UUID bookingId, int releasedRowCount, Instant releasedAt) {}
}
