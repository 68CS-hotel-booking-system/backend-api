package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import java.util.List;
import java.util.UUID;

public record ListCitiesResponse(UUID id, String countryCode, String name, String slug) {

  public static ListCitiesResponse from(ListCitiesResult result) {
    return new ListCitiesResponse(result.id(), result.countryCode(), result.name(), result.slug());
  }

  public static List<ListCitiesResponse> from(List<ListCitiesResult> results) {
    return results.stream().map(ListCitiesResponse::from).toList();
  }
}
