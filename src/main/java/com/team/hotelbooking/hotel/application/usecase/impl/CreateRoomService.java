package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.CreateRoomCommand;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.CreateRoomResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CreateRoomService implements CreateRoomUseCase {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;
  private final RoomRepository rooms;
  private final IdentityAccounts accounts;
  private final Clock clock;

  public CreateRoomService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      IdentityAccounts accounts,
      Clock clock) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.rooms = rooms;
    this.accounts = accounts;
    this.clock = clock;
  }

  @Override
  public CreateRoomResult execute(CreateRoomCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.roomTypeId() == null
        || command.operationalFrom() == null) {
      throw HotelException.invalid(
          "Thông tin người thực hiện, khách sạn, loại phòng và thời điểm bắt đầu phục vụ là bắt buộc.");
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

    RoomType roomType =
        roomTypes
            .findByIdAndHotelId(command.roomTypeId(), hotel.id())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND));

    if (!roomType.active()) {
      throw new HotelException(HotelErrorCode.ROOM_TYPE_INACTIVE);
    }

    Instant now = clock.instant();
    Room room =
        Room.create(
            UUID.randomUUID(),
            hotel.id(),
            roomType.id(),
            command.roomNumber(),
            command.floor(),
            command.operationalFrom(),
            command.operationalUntil(),
            actor.id(),
            now);

    Room saved = rooms.add(room);

    return CreateRoomResult.from(saved);
  }
}
