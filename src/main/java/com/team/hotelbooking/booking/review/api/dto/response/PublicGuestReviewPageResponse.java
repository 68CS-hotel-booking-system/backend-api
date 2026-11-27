package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.ListGuestReviewsResult;
import com.team.hotelbooking.shared.web.PageResponse;

public record PublicGuestReviewPageResponse(
    PageResponse<PublicGuestReviewResponse> reviews, GuestReviewSummaryResponse summary) {
  public static PublicGuestReviewPageResponse from(ListGuestReviewsResult r) {
    return new PublicGuestReviewPageResponse(
        PageResponse.from(r.reviews().map(PublicGuestReviewResponse::from)),
        GuestReviewSummaryResponse.from(r));
  }
}
