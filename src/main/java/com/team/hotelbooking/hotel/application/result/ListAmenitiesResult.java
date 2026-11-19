package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import java.util.UUID;

public record ListAmenitiesResult(UUID id, String code, String name, String category) {

  public static ListAmenitiesResult from(Amenity amenity) {
    return new ListAmenitiesResult(
        amenity.id(), amenity.code(), amenity.name(), amenity.category().name());
  }
}
