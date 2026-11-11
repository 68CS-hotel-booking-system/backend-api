package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ReplaceHotelAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ReplaceHotelAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ReplaceHotelAmenitiesService implements ReplaceHotelAmenitiesUseCase {

  private final HotelRepository hotels;
  private final AmenityRepository amenities;
  private final HotelManagementAccess access;

  public ReplaceHotelAmenitiesService(HotelRepository hotels, AmenityRepository amenities) {
    this.hotels = hotels;
    this.amenities = amenities;
    this.access = new HotelManagementAccess(hotels);
  }

  @Override
  public List<ListAmenitiesResult> execute(ReplaceHotelAmenitiesCommand command) {
    if (command == null || command.actorUserId() == null || command.hotelId() == null) {
      throw HotelException.invalid("Thông tin người thực hiện và khách sạn là bắt buộc.");
    }
    Set<UUID> amenityIds = requireAmenityIds(command.amenityIds());

    // Cùng quy tắc quyền với PATCH thông tin khách sạn; trạng thái tài khoản đã được
    // CurrentUserJwtConverter kiểm khi dựng principal (xem HotelManagementAccess).
    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());

    // Khóa hotel sau khi đã kiểm quyền để hai lần thay danh sách cùng hotel chạy tuần tự.
    hotels
        .findByIdForUpdate(command.hotelId())
        .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    Amenity.requireAssignable(
        amenityIds, amenities.findAllByIds(amenityIds), Amenity.Category.HOTEL);
    amenities.replaceHotelAmenities(command.hotelId(), amenityIds);

    return amenities.findByHotelId(command.hotelId()).stream()
        .map(ListAmenitiesResult::from)
        .toList();
  }

  // Danh sách rỗng nghĩa là gỡ hết tiện ích; id trùng chỉ tính một lần.
  static Set<UUID> requireAmenityIds(List<UUID> amenityIds) {
    if (amenityIds == null) {
      throw HotelException.invalid("Danh sách tiện ích là bắt buộc.");
    }
    if (amenityIds.stream().anyMatch(Objects::isNull)) {
      throw HotelException.invalid("Danh sách tiện ích không được chứa giá trị rỗng.");
    }
    return new LinkedHashSet<>(amenityIds);
  }
}
