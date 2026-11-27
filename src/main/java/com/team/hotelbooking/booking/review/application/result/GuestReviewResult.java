package com.team.hotelbooking.booking.review.application.result;

import com.team.hotelbooking.booking.review.domain.model.GuestReview;
import java.time.Instant;
import java.util.UUID;

public record GuestReviewResult(
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
  public static GuestReviewResult from(GuestReview r, boolean canReply) {
    return new GuestReviewResult(
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
        canReply);
  }
}
