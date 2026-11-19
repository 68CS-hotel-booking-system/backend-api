package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.util.List;
import java.util.UUID;

public record HotelSearchItemResponse(
    UUID id,
    String name,
    String slug,
    String address,
    Short starRating,
    CityResponse city,
    CoverImageResponse coverImage,
    Long minNightlyPrice,
    Long totalPrice,
    String currency,
    List<AmenityResponse> amenities) {

  public static HotelSearchItemResponse from(HotelSearchItemResult result) {
    return new HotelSearchItemResponse(
        result.id(),
        result.name(),
        result.slug(),
        result.address(),
        result.starRating(),
        new CityResponse(result.city().id(), result.city().name(), result.city().slug()),
        result.coverImage() == null ? null : new CoverImageResponse(result.coverImage().mediaId()),
        result.minNightlyPrice(),
        result.totalPrice(),
        result.currency(),
        result.amenities().stream()
            .map(amenity -> new AmenityResponse(amenity.id(), amenity.code(), amenity.name()))
            .toList());
  }

  public static PageResponse<HotelSearchItemResponse> from(
      PageResult<HotelSearchItemResult> result) {
    return new PageResponse<>(
        result.content().stream().map(HotelSearchItemResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.first(),
        result.last());
  }

  public record CityResponse(UUID id, String name, String slug) {}

  // Chỉ mediaId; FE gọi GET /api/v1/media/{mediaId}/access để lấy URL (hết hạn sau TTL).
  public record CoverImageResponse(UUID mediaId) {}

  public record AmenityResponse(UUID id, String code, String name) {}
}
