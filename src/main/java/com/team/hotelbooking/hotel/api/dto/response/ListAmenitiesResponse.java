package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import java.util.List;
import java.util.UUID;

public record ListAmenitiesResponse(UUID id, String code, String name, String category) {

  public static ListAmenitiesResponse from(ListAmenitiesResult result) {
    return new ListAmenitiesResponse(result.id(), result.code(), result.name(), result.category());
  }

  public static List<ListAmenitiesResponse> from(List<ListAmenitiesResult> results) {
    return results.stream().map(ListAmenitiesResponse::from).toList();
  }
}
