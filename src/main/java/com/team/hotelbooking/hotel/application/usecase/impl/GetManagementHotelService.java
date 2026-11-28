package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;

public class GetManagementHotelService implements GetManagementHotelUseCase {

  private final HotelRepository hotels;
  private final IdentityAccounts accounts;
  private final HotelMemberStore members;
  private final HotelManagementAccess access;

  public GetManagementHotelService(HotelRepository hotels, IdentityAccounts accounts) {
    this(hotels, accounts, null);
  }

  public GetManagementHotelService(
      HotelRepository hotels, IdentityAccounts accounts, HotelMemberStore members) {
    this.hotels = hotels;
    this.accounts = accounts;
    this.members = members;
    this.access = new HotelManagementAccess(hotels);
  }

  @Override
  public ManagementHotelResult execute(GetManagementHotelQuery query) {
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

    Hotel hotel =
        hotels
            .findById(query.hotelId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    HotelManagementAccess.Level level = access.levelOf(actor.id(), admin, hotel);
    if (level == HotelManagementAccess.Level.NONE) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }

    return ManagementHotelResult.from(hotel, level.name());
  }
}
