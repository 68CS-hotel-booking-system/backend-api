package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
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

public class ListManagementRoomsService implements ListManagementRoomsUseCase {

  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final IdentityAccounts accounts;
  private final int maxPageSize;

  public ListManagementRoomsService(
      HotelRepository hotels, RoomRepository rooms, IdentityAccounts accounts, int maxPageSize) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Maximum page size must be positive.");
    }
    this.hotels = hotels;
    this.rooms = rooms;
    this.accounts = accounts;
    this.maxPageSize = maxPageSize;
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
    if (!actor.active()
        || !actor.emailVerified()
        || (!admin && !actor.roles().contains("HOTEL_OWNER"))) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    // Owner chỉ thấy hotel của mình; hotel của người khác trả 404 như hotel không tồn tại.
    Hotel hotel =
        (admin
                ? hotels.findById(query.hotelId())
                : hotels.findByIdAndOwnerId(query.hotelId(), actor.id()))
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    // Lọc theo cả hotel_id: roomTypeId của hotel khác chỉ cho trang rỗng, không lộ dữ liệu.
    PageResult<Room> result =
        query.roomTypeId() == null
            ? rooms.findByHotelId(hotel.id(), pagination)
            : rooms.findByHotelIdAndRoomTypeId(hotel.id(), query.roomTypeId(), pagination);

    return result.map(ListManagementRoomsResult::from);
  }
}
