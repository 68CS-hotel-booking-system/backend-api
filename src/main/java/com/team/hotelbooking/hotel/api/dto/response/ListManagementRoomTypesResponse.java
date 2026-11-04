package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListManagementRoomTypesResponse(
    UUID id,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static ListManagementRoomTypesResponse from(ListManagementRoomTypesResult result) {
    return new ListManagementRoomTypesResponse(
        result.id(),
        result.name(),
        result.description(),
        result.maxAdults(),
        result.maxChildren(),
        result.active(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }

  public static List<ListManagementRoomTypesResponse> from(
      List<ListManagementRoomTypesResult> results) {
    return results.stream().map(ListManagementRoomTypesResponse::from).toList();
  }
}
