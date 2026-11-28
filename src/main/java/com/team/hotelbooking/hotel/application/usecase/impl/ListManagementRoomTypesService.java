package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
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
  private final HotelMemberStore members;
  private final HotelManagementAccess access;

  public ListManagementRoomTypesService(
      HotelRepository hotels, RoomTypeRepository roomTypes, IdentityAccounts accounts) {
    this(hotels, roomTypes, accounts, null);
  }

  public ListManagementRoomTypesService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      IdentityAccounts accounts,
      HotelMemberStore members) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.accounts = accounts;
    this.members = members;
    this.access = new HotelManagementAccess(hotels);
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

    return roomTypes.findByHotelId(hotel.id()).stream()
        .map(ListManagementRoomTypesResult::from)
        .toList();
  }
}
