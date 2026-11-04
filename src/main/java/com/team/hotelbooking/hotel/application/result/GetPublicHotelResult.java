package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import java.math.BigDecimal;
import java.time.LocalTime;
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
    LocalTime checkOutTime) {

  public static GetPublicHotelResult from(Hotel hotel) {
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
        hotel.checkOutTime());
  }
}
