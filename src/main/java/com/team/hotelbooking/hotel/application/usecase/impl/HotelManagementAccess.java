package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.util.Set;
import java.util.UUID;

/**
 * Quyền quản lý nội dung khách sạn (HT-09), nguồn duy nhất cho PATCH thông tin hotel, PUT tiện ích,
 * PATCH loại phòng, PATCH phòng và HotelDirectory.findManagementAccess (giá phòng của pricing):
 *
 * <ul>
 *   <li>ADMIN quản lý được mọi hotel; hotel không tồn tại → 404 HOTEL_NOT_FOUND.
 *   <li>Owner của hotel (hotels.owner_user_id) hoặc HOTEL_MANAGER đang hoạt động của đúng hotel đó
 *       được quản lý.
 *   <li>Thành viên đang hoạt động nhưng không đủ quyền (RECEPTIONIST) → 403 ACCESS_DENIED.
 *   <li>Còn lại (không phải Owner/thành viên, hoặc hotel không tồn tại) → 404 HOTEL_NOT_FOUND,
 *       giống GetManagementHotelService/ListManagementRoomsService, để không lộ hotel có tồn tại.
 * </ul>
 *
 * <p>Không đọc lại tài khoản qua IdentityAccounts: actorUserId và cờ admin lấy từ principal do
 * CurrentUserJwtConverter dựng. Converter đã chặn tài khoản BANNED/INACTIVE, kiểm auth_version và
 * nạp role hiện tại từ DB; tài khoản PENDING không đăng nhập được. Nên tại đây chỉ còn kiểm quan hệ
 * của người gọi với đúng hotel.
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
    Hotel hotel =
        hotels
            .findById(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

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
