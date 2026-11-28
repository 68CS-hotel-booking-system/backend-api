package com.team.hotelbooking.booking.review.domain.model;

import com.team.hotelbooking.booking.review.domain.exception.GuestReviewErrorCode;
import com.team.hotelbooking.booking.review.domain.exception.GuestReviewException;
import java.time.Instant;
import java.util.UUID;

public record GuestReview(
    UUID id,
    UUID bookingId,
    UUID customerId,
    UUID hotelId,
    String bookingCode,
    String authorDisplayName,
    int rating,
    String content,
    Instant createdAt,
    String reply,
    UUID repliedBy,
    Instant repliedAt,
    long version) {
  public static GuestReview submit(
      UUID bookingId,
      UUID customerId,
      UUID hotelId,
      String bookingCode,
      String authorName,
      Integer rating,
      String content,
      Instant now) {
    if (bookingId == null
        || customerId == null
        || hotelId == null
        || now == null
        || rating == null
        || rating < 1
        || rating > 5)
      throw new GuestReviewException(
          GuestReviewErrorCode.VALIDATION_FAILED, "Điểm đánh giá phải là số nguyên từ 1 đến 5.");
    String text = normalized(content);
    String[] names = (authorName == null ? "" : authorName.trim()).split("\\s+");
    String display =
        names.length > 1
            ? names[0].substring(0, 1) + ". " + names[names.length - 1]
            : "Khách lưu trú";
    return new GuestReview(
        UUID.randomUUID(),
        bookingId,
        customerId,
        hotelId,
        bookingCode,
        display,
        rating,
        text,
        now,
        null,
        null,
        null,
        0);
  }

  private static String normalized(String text) {
    String value = text == null ? "" : text.trim();
    if (value.isEmpty() || value.length() > 2000)
      throw new GuestReviewException(
          GuestReviewErrorCode.VALIDATION_FAILED, "Nội dung phải có từ 1 đến 2000 ký tự.");
    return value;
  }

  public GuestReview respond(UUID actor, String text, Instant now) {
    if (actor == null || now == null)
      throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    return new GuestReview(
        id,
        bookingId,
        customerId,
        hotelId,
        bookingCode,
        authorDisplayName,
        rating,
        content,
        createdAt,
        normalized(text),
        actor,
        now,
        version + 1);
  }
}
