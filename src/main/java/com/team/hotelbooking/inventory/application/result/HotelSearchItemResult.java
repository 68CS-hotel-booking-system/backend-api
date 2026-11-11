package com.team.hotelbooking.inventory.application.result;

import java.util.List;
import java.util.UUID;

/**
 * Một khách sạn trong kết quả tìm kiếm. Có ngày: minNightlyPrice = giá trung bình một đêm (làm tròn
 * xuống) của loại phòng rẻ nhất phù hợp, totalPrice = tổng giá một phòng cả kỳ của loại đó. Không
 * có ngày: minNightlyPrice = base_price nhỏ nhất (> 0) của loại phòng phù hợp, totalPrice = null.
 */
public record HotelSearchItemResult(
    UUID id,
    String name,
    String slug,
    String address,
    Short starRating,
    CityResult city,
    CoverImageResult coverImage,
    Long minNightlyPrice,
    Long totalPrice,
    String currency,
    List<AmenityResult> amenities) {

  public HotelSearchItemResult {
    amenities = List.copyOf(amenities);
  }

  public record CityResult(UUID id, String name, String slug) {}

  public record CoverImageResult(UUID mediaId) {}

  public record AmenityResult(UUID id, String code, String name) {}
}
