package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.ListGuestReviewsResult;
import com.team.hotelbooking.shared.web.PageResponse;

public record ManagedGuestReviewPageResponse(
    PageResponse<GuestReviewResponse> reviews, GuestReviewSummaryResponse summary) {
  public static ManagedGuestReviewPageResponse from(ListGuestReviewsResult r) {
    return new ManagedGuestReviewPageResponse(
        PageResponse.from(r.reviews().map(GuestReviewResponse::from)),
        GuestReviewSummaryResponse.from(r));
  }
}
