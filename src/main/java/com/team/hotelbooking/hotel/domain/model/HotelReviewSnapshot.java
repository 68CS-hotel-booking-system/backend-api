package com.team.hotelbooking.hotel.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Nội dung đã lưu tại lúc gửi; không chứa URL ảnh có thời hạn hay trạng thái buồng phòng. */
public record HotelReviewSnapshot(
    Hotel hotel,
    List<Amenity> amenities,
    List<CustomAmenity> customAmenities,
    List<RoomTypeSnapshot> roomTypes,
    List<RoomSnapshot> rooms,
    List<ImageSnapshot> images,
    List<AlbumSnapshot> albums) {
  public HotelReviewSnapshot {
    amenities = List.copyOf(amenities);
    customAmenities = List.copyOf(customAmenities);
    roomTypes = List.copyOf(roomTypes);
    rooms = List.copyOf(rooms);
    images = List.copyOf(images);
    albums = List.copyOf(albums);
  }

  public record RoomTypeSnapshot(RoomType roomType, List<Amenity> amenities) {
    public RoomTypeSnapshot {
      amenities = List.copyOf(amenities);
    }
  }

  public record RoomSnapshot(
      UUID id,
      UUID roomTypeId,
      String roomNumber,
      String floor,
      Instant operationalFrom,
      Instant operationalUntil,
      List<Amenity> amenities,
      List<CustomAmenity> customAmenities) {
    public RoomSnapshot {
      amenities = List.copyOf(amenities);
      customAmenities = List.copyOf(customAmenities);
    }
  }

  public record ImageSnapshot(
      UUID mediaId, UUID albumId, int sortOrder, boolean cover, String altText) {}

  public record AlbumSnapshot(UUID id, String name, String description, int sortOrder) {}
}
