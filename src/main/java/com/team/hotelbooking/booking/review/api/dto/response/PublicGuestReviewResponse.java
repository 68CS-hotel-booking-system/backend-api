package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.GuestReviewResult;
import java.time.Instant;
import java.util.UUID;

public record PublicGuestReviewResponse(
    UUID id,
    String authorDisplayName,
    int rating,
    String content,
    Instant createdAt,
    String reply,
    Instant repliedAt) {
  public static PublicGuestReviewResponse from(GuestReviewResult r) {
    return new PublicGuestReviewResponse(
        r.id(),
        r.authorDisplayName(),
        r.rating(),
        r.content(),
        r.createdAt(),
        r.reply(),
        r.repliedAt());
  }
}
