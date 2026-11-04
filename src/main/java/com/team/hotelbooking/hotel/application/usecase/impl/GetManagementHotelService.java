package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
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

  public GetManagementHotelService(HotelRepository hotels, IdentityAccounts accounts) {
    this.hotels = hotels;
    this.accounts = accounts;
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
    if (!actor.active()
        || !actor.emailVerified()
        || (!admin && !actor.roles().contains("HOTEL_OWNER"))) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    Hotel hotel =
        (admin
                ? hotels.findById(query.hotelId())
                : hotels.findByIdAndOwnerId(query.hotelId(), actor.id()))
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    return ManagementHotelResult.from(hotel);
  }
}
