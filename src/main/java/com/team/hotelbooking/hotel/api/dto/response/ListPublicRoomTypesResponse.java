package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import java.util.List;
import java.util.UUID;

public record ListPublicRoomTypesResponse(
    UUID id,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    List<ListAmenitiesResponse> amenities) {

  public static ListPublicRoomTypesResponse from(ListPublicRoomTypesResult result) {
    return new ListPublicRoomTypesResponse(
        result.id(),
        result.name(),
        result.description(),
        result.maxAdults(),
        result.maxChildren(),
        ListAmenitiesResponse.from(result.amenities()));
  }

  public static List<ListPublicRoomTypesResponse> from(List<ListPublicRoomTypesResult> results) {
    return results.stream().map(ListPublicRoomTypesResponse::from).toList();
  }
}
