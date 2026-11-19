package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.HotelReviewSummaryResult;
import java.time.Instant;
import java.util.UUID;

public record HotelReviewSummaryResponse(
    UUID id,
    UUID hotelId,
    String hotelName,
    String status,
    UUID submittedBy,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String reason,
    long version) {
  public static HotelReviewSummaryResponse from(HotelReviewSummaryResult result) {
    return new HotelReviewSummaryResponse(
        result.id(),
        result.hotelId(),
        result.hotelName(),
        result.status(),
        result.submittedBy(),
        result.submittedAt(),
        result.reviewedBy(),
        result.reviewedAt(),
        result.reason(),
        result.version());
  }
}
