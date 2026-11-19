package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetPublicHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.util.UUID;

public class GetPublicHotelService implements GetPublicHotelUseCase {

  private final HotelRepository hotels;
  private final AmenityRepository amenities;

  public GetPublicHotelService(HotelRepository hotels, AmenityRepository amenities) {
    this.hotels = hotels;
    this.amenities = amenities;
  }

  @Override
  public GetPublicHotelResult execute(UUID hotelId) {
    if (hotelId == null) {
      throw HotelException.invalid("Mã khách sạn là bắt buộc.");
    }

    // Lọc ACTIVE ngay khi truy vấn: hotel không tồn tại và hotel chưa công khai cùng trả
    // HOTEL_NOT_FOUND, để khách không suy ra được trạng thái nội bộ.
    Hotel hotel =
        hotels
            .findByIdAndStatus(hotelId, Hotel.Status.ACTIVE)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    return GetPublicHotelResult.from(hotel, amenities.findByHotelId(hotel.id()));
  }
}
