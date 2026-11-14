package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.HotelPublishReadinessResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HotelPublishReadinessResponse(
    UUID hotelId,
    boolean ready,
    long operationalRoomCount,
    Instant checkedAt,
    List<Requirement> unmetRequirements) {
  public record Requirement(String field, String message) {}

  public static HotelPublishReadinessResponse from(HotelPublishReadinessResult result) {
    return new HotelPublishReadinessResponse(
        result.hotelId(),
        result.ready(),
        result.operationalRoomCount(),
        result.checkedAt(),
        result.unmetRequirements().stream()
            .map(item -> new Requirement(item.field(), item.message()))
            .toList());
  }
}
