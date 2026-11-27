package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.GuestReviewResult;
import java.time.Instant;
import java.util.UUID;

public record GuestReviewResponse(
    UUID id,
    UUID bookingId,
    UUID hotelId,
    String bookingCode,
    String authorDisplayName,
    int rating,
    String content,
    Instant createdAt,
    String reply,
    Instant repliedAt,
    long version,
    boolean canReply) {
  public static GuestReviewResponse from(GuestReviewResult r) {
    return new GuestReviewResponse(
        r.id(),
        r.bookingId(),
        r.hotelId(),
        r.bookingCode(),
        r.authorDisplayName(),
        r.rating(),
        r.content(),
        r.createdAt(),
        r.reply(),
        r.repliedAt(),
        r.version(),
        r.canReply());
  }
}
