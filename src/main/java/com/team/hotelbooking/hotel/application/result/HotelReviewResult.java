package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot;
import java.time.Instant;
import java.util.UUID;

public record HotelReviewResult(
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
  public static HotelReviewResult from(HotelReviewRequest request) {
    return new HotelReviewResult(
        request.id(),
        request.hotelId(),
        request.snapshot(),
        request.status().name(),
        request.submittedBy(),
        request.submittedAt(),
        request.reviewedBy(),
        request.reviewedAt(),
        request.reason(),
        request.version());
  }
}
