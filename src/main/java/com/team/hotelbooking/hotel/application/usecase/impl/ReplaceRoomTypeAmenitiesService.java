package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ReplaceRoomTypeAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReplaceRoomTypeAmenitiesService implements ReplaceRoomTypeAmenitiesUseCase {

  private final RoomTypeRepository roomTypes;
  private final AmenityRepository amenities;
  private final HotelManagementAccess access;

  public ReplaceRoomTypeAmenitiesService(
      HotelRepository hotels, RoomTypeRepository roomTypes, AmenityRepository amenities) {
    this.roomTypes = roomTypes;
    this.amenities = amenities;
    this.access = new HotelManagementAccess(hotels);
  }

  @Override
  public List<ListAmenitiesResult> execute(ReplaceRoomTypeAmenitiesCommand command) {
    if (command == null
        || command.actorUserId() == null
        || command.hotelId() == null
        || command.roomTypeId() == null) {
      throw HotelException.invalid(
          "Thông tin người thực hiện, khách sạn và loại phòng là bắt buộc.");
    }
    Set<UUID> amenityIds = ReplaceHotelAmenitiesService.requireAmenityIds(command.amenityIds());

    // Cùng quy tắc quyền với PATCH thông tin khách sạn; trạng thái tài khoản đã được
    // CurrentUserJwtConverter kiểm khi dựng principal (xem HotelManagementAccess).
    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());

    // Khóa loại phòng (đúng hotel) để hai lần thay danh sách cùng loại phòng chạy tuần tự.
    roomTypes
        .findByIdAndHotelIdForUpdate(command.roomTypeId(), command.hotelId())
        .orElseThrow(() -> new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND));

    Amenity.requireAssignable(
        amenityIds, amenities.findAllByIds(amenityIds), Amenity.Category.ROOM);
    amenities.replaceRoomTypeAmenities(command.roomTypeId(), amenityIds);

    return amenities.findByRoomTypeId(command.roomTypeId()).stream()
        .map(ListAmenitiesResult::from)
        .toList();
  }
}
