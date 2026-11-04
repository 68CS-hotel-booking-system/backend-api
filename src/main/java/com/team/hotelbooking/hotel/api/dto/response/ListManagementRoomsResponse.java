package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.UUID;

public record ListManagementRoomsResponse(
    UUID id,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    String readiness,
    Instant readinessUpdatedAt,
    Instant operationalFrom,
    Instant operationalUntil,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static ListManagementRoomsResponse from(ListManagementRoomsResult result) {
    return new ListManagementRoomsResponse(
        result.id(),
        result.roomTypeId(),
        result.roomNumber(),
        result.floor(),
        result.readiness(),
        result.readinessUpdatedAt(),
        result.operationalFrom(),
        result.operationalUntil(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }

  public static PageResponse<ListManagementRoomsResponse> from(
      PageResult<ListManagementRoomsResult> result) {
    return PageResponse.from(result.map(ListManagementRoomsResponse::from));
  }
}
