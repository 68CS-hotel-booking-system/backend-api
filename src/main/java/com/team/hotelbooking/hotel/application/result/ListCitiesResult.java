package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.City;
import java.util.UUID;

public record ListCitiesResult(UUID id, String countryCode, String name, String slug) {

  public static ListCitiesResult from(City city) {
    return new ListCitiesResult(city.id(), city.countryCode(), city.name(), city.slug());
  }
}
