package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

// Một dòng trong danh sách khách sạn công khai; tách khỏi GetPublicHotelResult để danh sách không
// phải nạp tiện ích từng khách sạn. Không mang owner, trạng thái hay metadata nội bộ ra ngoài.
public record ListPublicHotelsResult(
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

  public static ListPublicHotelsResult from(Hotel hotel) {
    return new ListPublicHotelsResult(
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
