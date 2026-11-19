package com.team.hotelbooking.pricing.application.service;

import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelManagement;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import java.util.UUID;

/**
 * Quyền quản lý giá phòng theo hotel. Mức quyền do module hotel quyết định (cùng quy tắc PATCH
 * hotel); pricing chỉ đổi thành lỗi của mình:
 *
 * <ul>
 *   <li>ADMIN/Owner/HOTEL_MANAGER đúng hotel → được quản lý giá.
 *   <li>RECEPTIONIST → 403 ACCESS_DENIED.
 *   <li>Người ngoài hoặc hotel không tồn tại → 404 HOTEL_NOT_FOUND, không lộ hotel có tồn tại.
 * </ul>
 *
 * <p>Giá được chuẩn bị cả khi hotel còn DRAFT hoặc đang SUSPENDED để sẵn sàng mở bán; hotel bị quản
 * trị khóa (LOCKED) thì chặn mọi thao tác giá cho tới khi gỡ khóa (409 HOTEL_LOCKED).
 */
final class PricingManagementAccess {

  private PricingManagementAccess() {}

  static void require(PricingHotelPort hotels, UUID hotelId, UUID actorUserId, boolean admin) {
    HotelManagement access =
        hotels
            .findManagementAccess(hotelId, actorUserId, admin)
            .orElseThrow(() -> PricingException.hotelNotFound("Không tìm thấy khách sạn."));

    switch (access.level()) {
      case ADMIN, OWNER, MANAGER -> {}
      case RECEPTIONIST -> throw new PricingException(PricingErrorCode.ACCESS_DENIED);
      case NONE -> throw PricingException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    if (access.status() == PricingHotelPort.HotelStatus.LOCKED) {
      throw new PricingException(PricingErrorCode.HOTEL_LOCKED);
    }
  }
}
