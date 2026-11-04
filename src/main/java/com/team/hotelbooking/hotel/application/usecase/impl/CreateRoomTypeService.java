package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.CreateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.CreateRoomTypeResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CreateRoomTypeService implements CreateRoomTypeUseCase {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;
  private final IdentityAccounts accounts;
  private final Clock clock;

  public CreateRoomTypeService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      IdentityAccounts accounts,
      Clock clock) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.accounts = accounts;
    this.clock = clock;
  }

  @Override
  public CreateRoomTypeResult execute(CreateRoomTypeCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.maxAdults() == null
        || command.maxChildren() == null) {
      throw HotelException.invalid("Thông tin người thực hiện, khách sạn và sức chứa là bắt buộc.");
    }

    AccountSnapshot actor =
        accounts
            .findById(command.actorId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    boolean admin = actor.roles().contains("ADMIN");
    if (!actor.active()
        || !actor.emailVerified()
        || (!admin && !actor.roles().contains("HOTEL_OWNER"))) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    Hotel hotel =
        (admin
                ? hotels.findById(command.hotelId())
                : hotels.findByIdAndOwnerId(command.hotelId(), actor.id()))
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    Instant now = clock.instant();
    RoomType roomType =
        RoomType.create(
            UUID.randomUUID(),
            hotel.id(),
            command.name(),
            command.description(),
            command.maxAdults(),
            command.maxChildren(),
            now);

    RoomType saved = roomTypes.add(roomType);

    return CreateRoomTypeResult.from(saved);
  }
}
