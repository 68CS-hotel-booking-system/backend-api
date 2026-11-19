package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.HotelReviewSummary;
import java.time.Instant;
import java.util.UUID;

public record HotelReviewSummaryResult(
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
  public static HotelReviewSummaryResult from(HotelReviewSummary item) {
    return new HotelReviewSummaryResult(
        item.id(),
        item.hotelId(),
        item.hotelName(),
        item.status().name(),
        item.submittedBy(),
        item.submittedAt(),
        item.reviewedBy(),
        item.reviewedAt(),
        item.reason(),
        item.version());
  }
}
