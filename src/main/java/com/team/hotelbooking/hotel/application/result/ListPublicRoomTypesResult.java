package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.util.UUID;

public record ListPublicRoomTypesResult(
    UUID id, String name, String description, short maxAdults, short maxChildren) {

  public static ListPublicRoomTypesResult from(RoomType roomType) {
    return new ListPublicRoomTypesResult(
        roomType.id(),
        roomType.name(),
        roomType.description(),
        roomType.maxAdults(),
        roomType.maxChildren());
  }
}
