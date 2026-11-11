package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.GetRoomTypeAmenitiesQuery;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.GetRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.util.List;

public final class GetRoomTypeAmenitiesService implements GetRoomTypeAmenitiesUseCase {
  private final HotelManagementAccess access;
  private final RoomTypeRepository roomTypes;
  private final AmenityRepository amenities;

  public GetRoomTypeAmenitiesService(
      HotelRepository hotels, RoomTypeRepository roomTypes, AmenityRepository amenities) {
    this.access = new HotelManagementAccess(hotels);
    this.roomTypes = roomTypes;
    this.amenities = amenities;
  }

  @Override
  public List<ListAmenitiesResult> execute(GetRoomTypeAmenitiesQuery query) {
    if (query == null
        || query.actorUserId() == null
        || query.hotelId() == null
        || query.roomTypeId() == null)
      throw HotelException.invalid("Người thực hiện, khách sạn và hạng phòng là bắt buộc.");
    access.requireEditableHotel(query.actorUserId(), query.admin(), query.hotelId());
    roomTypes
        .findByIdAndHotelId(query.roomTypeId(), query.hotelId())
        .orElseThrow(() -> new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND));
    return amenities.findByRoomTypeId(query.roomTypeId()).stream()
        .map(ListAmenitiesResult::from)
        .toList();
  }
}
