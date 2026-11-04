package com.team.hotelbooking.hotel.domain.model;

import java.util.UUID;

public record Amenity(UUID id, String code, String name, Category category) {

  public enum Category {
    HOTEL,
    ROOM
  }
}
