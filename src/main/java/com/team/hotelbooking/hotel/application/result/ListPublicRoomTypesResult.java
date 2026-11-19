package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.util.List;
import java.util.UUID;

public record ListPublicRoomTypesResult(
    UUID id,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    List<ListAmenitiesResult> amenities) {

  public ListPublicRoomTypesResult {
    amenities = List.copyOf(amenities);
  }

  public static ListPublicRoomTypesResult from(RoomType roomType, List<Amenity> amenities) {
    return new ListPublicRoomTypesResult(
        roomType.id(),
        roomType.name(),
        roomType.description(),
        roomType.maxAdults(),
        roomType.maxChildren(),
        amenities.stream().map(ListAmenitiesResult::from).toList());
  }
}
