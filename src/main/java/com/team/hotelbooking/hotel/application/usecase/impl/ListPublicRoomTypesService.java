package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.ListPublicRoomTypesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.util.List;
import java.util.UUID;

public class ListPublicRoomTypesService implements ListPublicRoomTypesUseCase {

  private final HotelRepository hotels;
  private final RoomTypeRepository roomTypes;

  public ListPublicRoomTypesService(HotelRepository hotels, RoomTypeRepository roomTypes) {
    this.hotels = hotels;
    this.roomTypes = roomTypes;
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

    return roomTypes.findActiveByHotelId(hotelId).stream()
        .map(ListPublicRoomTypesResult::from)
        .toList();
  }
}
