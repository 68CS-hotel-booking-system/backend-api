package com.team.hotelbooking.booking.review.application.result;

import com.team.hotelbooking.shared.pagination.PageResult;
import java.math.BigDecimal;
import java.util.List;

public record ListGuestReviewsResult(
    PageResult<GuestReviewResult> reviews,
    long totalReviews,
    BigDecimal averageRating,
    List<RatingCount> ratingCounts) {
  public record RatingCount(int rating, long count) {}
}
