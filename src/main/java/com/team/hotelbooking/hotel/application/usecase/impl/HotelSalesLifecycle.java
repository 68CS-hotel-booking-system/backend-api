package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelPublishRequirement;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.domain.service.HotelPublishReadiness;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bước dùng chung của approve/suspend/resume/lock/unlock: khóa hàng hotel, kiểm HT-03 khi chuyển
 * sang ACTIVE (một hàm chung cho approve và resume), rồi ghi trạng thái cùng một dòng lịch sử trong
 * cùng transaction của use case.
 *
 * <p>Đổi trạng thái chỉ chặn hoặc mở đơn mới; không hủy hay hoàn tiền đơn đã có (HT-06).
 */
final class HotelSalesLifecycle {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;
  private final RoomRepository rooms;
  private final HotelStatusHistoryRepository history;

  HotelSalesLifecycle(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.rooms = rooms;
    this.history = history;
  }

  // Khóa ghi để hai yêu cầu đổi trạng thái cùng hotel chạy tuần tự và đọc trạng thái mới nhất.
  Hotel lockForStatusChange(UUID hotelId) {
    return hotels
        .findByIdForUpdate(hotelId)
        .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
  }

  void requireReadyToSell(Hotel hotel, Instant now) {
    List<HotelPublishRequirement> unmet =
        HotelPublishReadiness.unmetRequirements(
            hotel,
            roomTypes.findByHotelId(hotel.id()),
            rooms.countOperationalRoomsByRoomType(hotel.id(), now));
    if (!unmet.isEmpty()) {
      throw HotelException.notReady(unmet);
    }
  }

  Hotel.Status statusBeforeLatestLock(UUID hotelId) {
    // Không còn lịch sử khóa (dữ liệu sửa tay): coi như chưa từng công khai để quản lý không tự
    // mở bán được; quản trị phải duyệt (approve) lại.
    return history
        .findLatestInto(hotelId, Hotel.Status.LOCKED)
        .map(HotelStatusChange::fromStatus)
        .orElse(Hotel.Status.DRAFT);
  }

  ManagementHotelResult record(
      Hotel before, Hotel after, String reason, boolean reasonRequired, UUID actorUserId) {
    // Kiểm lý do trước khi ghi để lỗi dữ liệu không để lại trạng thái đã đổi.
    HotelStatusChange change =
        HotelStatusChange.of(UUID.randomUUID(), before, after, reason, reasonRequired, actorUserId);
    Hotel saved = hotels.updateStatus(after);
    history.add(change);
    return ManagementHotelResult.from(saved);
  }
}
