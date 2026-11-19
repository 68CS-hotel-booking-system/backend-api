package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// Chỉ giữ thông tin công khai; không mang owner, trạng thái hay metadata nội bộ ra ngoài.
public record GetPublicHotelResult(
    UUID id,
    UUID cityId,
    String name,
    String slug,
    String description,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    List<ListAmenitiesResult> amenities) {

  public GetPublicHotelResult {
    amenities = List.copyOf(amenities);
  }

  public static GetPublicHotelResult from(Hotel hotel, List<Amenity> amenities) {
    return new GetPublicHotelResult(
        hotel.id(),
        hotel.cityId(),
        hotel.name(),
        hotel.slug(),
        hotel.description(),
        hotel.address(),
        hotel.latitude(),
        hotel.longitude(),
        hotel.starRating(),
        hotel.checkInTime(),
        hotel.checkOutTime(),
        amenities.stream().map(ListAmenitiesResult::from).toList());
  }
}
