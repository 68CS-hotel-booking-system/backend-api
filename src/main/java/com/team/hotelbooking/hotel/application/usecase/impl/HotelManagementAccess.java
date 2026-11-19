package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.util.Set;
import java.util.UUID;

/**
 * Quyền theo khách sạn. decide/requireManageableHotel phục vụ đọc, giá và vận hành; giữ quyền
 * Admin. requireEditableHotel chỉ cho Owner/Manager sửa nội dung, khóa hotel trước bản ghi con và
 * chặn mọi ghi hồ sơ trong khi có review PENDING. Receptionist nhận 403; người ngoài nhận 404.
 * Principal đã được CurrentUserJwtConverter kiểm trạng thái tài khoản và role hiện tại.
 */
public final class HotelManagementAccess {

  public enum Decision {
    GRANTED,
    DENIED,
    NOT_FOUND
  }

  // Mức quan hệ của người gọi với một hotel; ưu tiên từ cao xuống thấp.
  public enum Level {
    ADMIN,
    OWNER,
    MANAGER,
    RECEPTIONIST,
    NONE
  }

  private final HotelRepository hotels;

  public HotelManagementAccess(HotelRepository hotels) {
    this.hotels = hotels;
  }

  public Decision decide(UUID actorUserId, boolean admin, UUID hotelId) {
    return hotels
        .findById(hotelId)
        .map(hotel -> decide(actorUserId, admin, hotel))
        .orElse(Decision.NOT_FOUND);
  }

  public Hotel requireEditableHotel(UUID actorUserId, boolean admin, UUID hotelId) {
    if (admin) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    // Parent trước mọi bản ghi con: gửi duyệt và sửa hồ sơ không thể đọc/ghi xen kẽ.
    Hotel hotel =
        hotels
            .findByIdForUpdate(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
    requireAccess(actorUserId, false, hotel);
    if (hotels.hasPendingReview(hotelId)) {
      throw new HotelException(HotelErrorCode.REVIEW_PENDING);
    }
    return hotel;
  }

  // Đọc và vận hành lưu trú giữ quyền quản trị cũ, không bị khóa bởi hồ sơ PENDING.
  public Hotel requireManageableHotel(UUID actorUserId, boolean admin, UUID hotelId) {
    Hotel hotel =
        hotels
            .findById(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    return requireAccess(actorUserId, admin, hotel);
  }

  private Hotel requireAccess(UUID actorUserId, boolean admin, Hotel hotel) {
    return switch (decide(actorUserId, admin, hotel)) {
      case GRANTED -> hotel;
      case DENIED -> throw new HotelException(HotelErrorCode.ACCESS_DENIED);
      case NOT_FOUND -> throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    };
  }

  public Level levelOf(UUID actorUserId, boolean admin, Hotel hotel) {
    if (admin) {
      return Level.ADMIN;
    }
    if (hotel.ownerId().equals(actorUserId)) {
      return Level.OWNER;
    }

    Set<HotelMemberRole> roles = hotels.findActiveMemberRoles(hotel.id(), actorUserId);
    if (roles.contains(HotelMemberRole.HOTEL_MANAGER)) {
      return Level.MANAGER;
    }
    return roles.contains(HotelMemberRole.RECEPTIONIST) ? Level.RECEPTIONIST : Level.NONE;
  }

  private Decision decide(UUID actorUserId, boolean admin, Hotel hotel) {
    return switch (levelOf(actorUserId, admin, hotel)) {
      case ADMIN, OWNER, MANAGER -> Decision.GRANTED;
      case RECEPTIONIST -> Decision.DENIED;
      case NONE -> Decision.NOT_FOUND;
    };
  }
}
