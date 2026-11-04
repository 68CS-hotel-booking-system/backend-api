package com.team.hotelbooking.hotel.application.service;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.time.Clock;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateRoomTypeService implements UpdateRoomTypeUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;
  private final Clock clock;

  @Override
  public RoomTypeResult execute(UpdateRoomTypeCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    if (!hotelJpaRepository.existsById(command.hotelId())) {
      throw HotelException.hotelNotFound("Không tìm thấy khách sạn với ID: " + command.hotelId());
    }

    RoomTypeJpaEntity roomType =
        roomTypeJpaRepository
            .findByIdAndHotelId(command.roomTypeId(), command.hotelId())
            .orElseThrow(
                () ->
                    HotelException.roomTypeNotFound(
                        "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

    if (command.name() != null) {
      String trimmedName = command.name().trim();
      if (trimmedName.isEmpty()) {
        throw HotelException.invalidRequest("Tên loại phòng không được để trống.");
      }
      roomType.setName(trimmedName);
    }

    if (command.description() != null) {
      roomType.setDescription(command.description().trim());
    }

    short newAdults = command.maxAdults() != null ? command.maxAdults() : roomType.getMaxAdults();
    short newChildren =
        command.maxChildren() != null ? command.maxChildren() : roomType.getMaxChildren();
    short newOccupancy =
        command.maxOccupancy() != null ? command.maxOccupancy() : roomType.getMaxOccupancy();

    if (newAdults < 1) {
      throw HotelException.invalidOccupancy("Số người lớn tối đa phải ít nhất là 1.");
    }
    if (newChildren < 0) {
      throw HotelException.invalidOccupancy("Số trẻ em tối đa không được âm.");
    }
    if (newOccupancy < 1) {
      throw HotelException.invalidOccupancy("Sức chứa tối đa phải ít nhất là 1.");
    }
    if (newAdults > newOccupancy) {
      throw HotelException.invalidOccupancy(
          "Số người lớn ("
              + newAdults
              + ") không được vượt quá sức chứa tối đa ("
              + newOccupancy
              + ").");
    }

    roomType.setMaxAdults(newAdults);
    roomType.setMaxChildren(newChildren);
    roomType.setMaxOccupancy(newOccupancy);

    if (command.basePrice() != null) {
      if (command.basePrice() < 0) {
        throw HotelException.invalidPrice("Giá cơ bản phải lớn hơn hoặc bằng 0.");
      }
      roomType.setBasePrice(command.basePrice());
    }

    if (command.active() != null) {
      roomType.setActive(command.active());
    }

    roomType.setUpdatedAt(clock.instant());

    RoomTypeJpaEntity saved = roomTypeJpaRepository.save(roomType);

    return new RoomTypeResult(
        saved.getId(),
        saved.getHotelId(),
        saved.getName(),
        saved.getDescription(),
        saved.getMaxAdults(),
        saved.getMaxChildren(),
        saved.getMaxOccupancy(),
        saved.getBasePrice(),
        saved.isActive(),
        saved.getCreatedAt(),
        saved.getUpdatedAt());
  }
}
