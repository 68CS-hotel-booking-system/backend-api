package com.team.hotelbooking.booking.review.domain.repository;

import com.team.hotelbooking.booking.review.domain.model.GuestReview;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.math.BigDecimal;
import java.util.*;

public interface GuestReviewRepository {
  Optional<GuestReview> findByBookingId(UUID id);

  Optional<GuestReview> findById(UUID id);

  GuestReview add(GuestReview review);

  boolean updateReply(GuestReview review, long expectedVersion);

  PageResult<GuestReview> list(UUID hotelId, Integer rating, Boolean replied, PageQuery page);

  Summary summarize(UUID hotelId);

  record RatingCount(int rating, long count) {}

  record Summary(long totalReviews, BigDecimal averageRating, List<RatingCount> ratingCounts) {}
}
