package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.HotelReviewResult;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot;
import java.time.Instant;
import java.util.UUID;

public record HotelReviewResponse(
    UUID id,
    UUID hotelId,
    HotelReviewSnapshot snapshot,
    String status,
    UUID submittedBy,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String reason,
    long version) {
  public static HotelReviewResponse from(HotelReviewResult result) {
    return new HotelReviewResponse(
        result.id(),
        result.hotelId(),
        result.snapshot(),
        result.status(),
        result.submittedBy(),
        result.submittedAt(),
        result.reviewedBy(),
        result.reviewedAt(),
        result.reason(),
        result.version());
  }
}
