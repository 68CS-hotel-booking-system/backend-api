package com.team.hotelbooking.hotel.application.service;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import java.time.Clock;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateRoomService implements UpdateRoomUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomJpaRepository roomJpaRepository;
  private final Clock clock;

  @Override
  public RoomResult execute(UpdateRoomCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    if (!hotelJpaRepository.existsById(command.hotelId())) {
      throw HotelException.hotelNotFound("Không tìm thấy khách sạn với ID: " + command.hotelId());
    }

    RoomJpaEntity room =
        roomJpaRepository
            .findByIdAndHotelId(command.roomId(), command.hotelId())
            .orElseThrow(
                () ->
                    HotelException.roomNotFound(
                        "Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."));

    if (command.roomNumber() != null) {
      String trimmed = command.roomNumber().trim();
      if (trimmed.isEmpty()) {
        throw HotelException.invalidRequest("Số phòng không được để trống.");
      }
      if (!trimmed.equalsIgnoreCase(room.getRoomNumber())) {
        if (roomJpaRepository.existsByHotelIdAndRoomNumberAndIdNot(
            command.hotelId(), trimmed, command.roomId())) {
          throw HotelException.duplicateRoomNumber(
              "Số phòng '" + trimmed + "' đã tồn tại trong khách sạn này.");
        }
        room.setRoomNumber(trimmed);
      }
    }

    if (command.floor() != null) {
      room.setFloor(String.valueOf(command.floor()));
    }

    if (command.status() != null) {
      try {
        RoomJpaEntity.DbRoomStatus parsedStatus =
            RoomJpaEntity.DbRoomStatus.valueOf(command.status().trim().toLowerCase());
        room.setStatus(parsedStatus);
      } catch (IllegalArgumentException ex) {
        throw HotelException.invalidRequest(
            "Trạng thái buồng phòng không hợp lệ. Chỉ chấp nhận: ready, cleaning, maintenance.");
      }
    }

    if (command.active() != null) {
      room.setActive(command.active());
    }

    room.setUpdatedAt(clock.instant());

    RoomJpaEntity saved = roomJpaRepository.save(room);

    Short floorNumber = null;
    if (saved.getFloor() != null) {
      try {
        floorNumber = Short.valueOf(saved.getFloor());
      } catch (NumberFormatException ignored) {
      }
    }

    return new RoomResult(
        saved.getId(),
        saved.getHotelId(),
        saved.getRoomTypeId(),
        saved.getRoomNumber(),
        floorNumber,
        saved.getStatus().name(),
        saved.isActive(),
        saved.getCreatedAt(),
        saved.getUpdatedAt());
  }
}
