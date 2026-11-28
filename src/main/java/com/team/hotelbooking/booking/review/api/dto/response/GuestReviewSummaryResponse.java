package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.ListGuestReviewsResult;
import java.math.BigDecimal;
import java.util.List;

public record GuestReviewSummaryResponse(
    long totalReviews, BigDecimal averageRating, List<RatingCount> ratingCounts) {
  public record RatingCount(int rating, long count) {}

  public static GuestReviewSummaryResponse from(ListGuestReviewsResult r) {
    return new GuestReviewSummaryResponse(
        r.totalReviews(),
        r.averageRating(),
        r.ratingCounts().stream().map(c -> new RatingCount(c.rating(), c.count())).toList());
  }
}
