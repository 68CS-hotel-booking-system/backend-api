package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.util.Objects;

public class UpdateRoomTypeService implements UpdateRoomTypeUseCase {

  private final HotelManagementAccess access;
  private final RoomTypeRepository roomTypes;
  private final Clock clock;

  public UpdateRoomTypeService(HotelRepository hotels, RoomTypeRepository roomTypes, Clock clock) {
    this.access = new HotelManagementAccess(hotels);
    this.roomTypes = roomTypes;
    this.clock = clock;
  }

  @Override
  public RoomTypeResult execute(UpdateRoomTypeCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    // Kiểm quyền theo hotel trước, rồi mới xét bản ghi con có thuộc hotel hay không.
    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());

    RoomType roomType =
        roomTypes
            .findByIdAndHotelId(command.roomTypeId(), command.hotelId())
            .orElseThrow(
                () ->
                    HotelException.roomTypeNotFound(
                        "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

    RoomType updated =
        roomType.update(
            command.name(),
            command.description(),
            command.maxAdults(),
            command.maxChildren(),
            command.maxOccupancy(),
            command.basePrice(),
            command.active(),
            clock.instant());

    return RoomTypeResult.from(roomTypes.update(updated));
  }
}
