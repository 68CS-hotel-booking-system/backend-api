package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomsUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.time.Instant;

public class ListManagementRoomsService implements ListManagementRoomsUseCase {

  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final IdentityAccounts accounts;
  private final HotelMemberStore members;
  private final HotelManagementAccess access;
  private final int maxPageSize;
  private final Clock clock;

  public ListManagementRoomsService(
      HotelRepository hotels,
      RoomRepository rooms,
      IdentityAccounts accounts,
      int maxPageSize,
      Clock clock) {
    this(hotels, rooms, accounts, null, maxPageSize, clock);
  }

  public ListManagementRoomsService(
      HotelRepository hotels,
      RoomRepository rooms,
      IdentityAccounts accounts,
      HotelMemberStore members,
      int maxPageSize,
      Clock clock) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Maximum page size must be positive.");
    }
    this.hotels = hotels;
    this.rooms = rooms;
    this.accounts = accounts;
    this.members = members;
    this.access = new HotelManagementAccess(hotels);
    this.maxPageSize = maxPageSize;
    this.clock = clock;
  }

  @Override
  public PageResult<ListManagementRoomsResult> execute(ListManagementRoomsQuery query) {
    if (query == null
        || query.actorId() == null
        || query.hotelId() == null
        || query.pagination() == null) {
      throw HotelException.invalid(
          "Thông tin người thực hiện, khách sạn và phân trang là bắt buộc.");
    }

    PageQuery pagination = query.pagination();
    if (pagination.page() < 0 || pagination.size() < 1 || pagination.size() > maxPageSize) {
      throw HotelException.invalid(
          "Số trang không được âm và kích thước trang phải từ 1 đến " + maxPageSize + ".");
    }
    if ((long) pagination.page() * pagination.size() > Integer.MAX_VALUE) {
      throw HotelException.invalid("Trang yêu cầu vượt quá phạm vi phân trang được hỗ trợ.");
    }

    AccountSnapshot actor =
        accounts
            .findById(query.actorId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    boolean admin = actor.roles().contains("ADMIN");
    boolean ownerRole = actor.roles().contains("HOTEL_OWNER");
    boolean hasStaff =
        !admin && !ownerRole && members != null && members.hasAnyActiveMembership(actor.id());
    if (!actor.active() || !actor.emailVerified() || (!admin && !ownerRole && !hasStaff)) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    // Owner/nhân sự chỉ thấy hotel của mình; hotel của người khác trả 404 như hotel không tồn tại.
    Hotel hotel =
        (!admin && ownerRole
                ? hotels.findByIdAndOwnerId(query.hotelId(), actor.id())
                : java.util.Optional.<Hotel>empty())
            .orElseGet(
                () -> {
                  Hotel found =
                      hotels
                          .findById(query.hotelId())
                          .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
                  if (access.levelOf(actor.id(), admin, found)
                      == HotelManagementAccess.Level.NONE) {
                    throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
                  }
                  return found;
                });

    // Lọc theo cả hotel_id: roomTypeId của hotel khác chỉ cho trang rỗng, không lộ dữ liệu.
    PageResult<Room> result =
        query.roomTypeId() == null
            ? rooms.findByHotelId(hotel.id(), pagination)
            : rooms.findByHotelIdAndRoomTypeId(hotel.id(), query.roomTypeId(), pagination);

    Instant at = clock.instant();
    return result.map(room -> ListManagementRoomsResult.from(room, at));
  }
}
