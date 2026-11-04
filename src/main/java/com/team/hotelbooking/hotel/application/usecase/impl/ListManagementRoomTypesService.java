package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomTypesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.util.List;

public class ListManagementRoomTypesService implements ListManagementRoomTypesUseCase {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;
  private final IdentityAccounts accounts;

  public ListManagementRoomTypesService(
      HotelRepository hotels, RoomTypeRepository roomTypes, IdentityAccounts accounts) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.accounts = accounts;
  }

  @Override
  public List<ListManagementRoomTypesResult> execute(ListManagementRoomTypesQuery query) {
    if (query == null || query.actorId() == null || query.hotelId() == null) {
      throw HotelException.invalid("Thông tin người thực hiện và khách sạn là bắt buộc.");
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

    return roomTypes.findByHotelId(hotel.id()).stream()
        .map(ListManagementRoomTypesResult::from)
        .toList();
  }
}
