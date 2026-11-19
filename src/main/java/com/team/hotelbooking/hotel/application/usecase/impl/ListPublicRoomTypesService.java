package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.ListPublicRoomTypesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ListPublicRoomTypesService implements ListPublicRoomTypesUseCase {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;
  private final AmenityRepository amenities;

  public ListPublicRoomTypesService(
      HotelRepository hotels, RoomTypeRepository roomTypes, AmenityRepository amenities) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
    this.amenities = amenities;
  }

  @Override
  public List<ListPublicRoomTypesResult> execute(UUID hotelId) {
    if (hotelId == null) {
      throw HotelException.invalid("Mã khách sạn là bắt buộc.");
    }

    // Kiểm tra hotel giống HT04 để hotel chưa công khai và hotel không tồn tại trả cùng một lỗi.
    if (hotels.findByIdAndStatus(hotelId, Hotel.Status.ACTIVE).isEmpty()) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }

    List<RoomType> activeRoomTypes = roomTypes.findActiveByHotelId(hotelId);
    if (activeRoomTypes.isEmpty()) {
      return List.of();
    }

    // Đọc tiện ích của mọi loại phòng trong một lần, tránh truy vấn theo từng loại phòng.
    Map<UUID, List<Amenity>> amenitiesByRoomType =
        amenities.findByRoomTypeIds(activeRoomTypes.stream().map(RoomType::id).toList());

    return activeRoomTypes.stream()
        .map(
            roomType ->
                ListPublicRoomTypesResult.from(
                    roomType, amenitiesByRoomType.getOrDefault(roomType.id(), List.of())))
        .toList();
  }
}
