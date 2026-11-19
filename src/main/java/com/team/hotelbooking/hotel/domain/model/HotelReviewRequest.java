package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;

public record HotelReviewRequest(
    UUID id,
    UUID hotelId,
    HotelReviewSnapshot snapshot,
    Status status,
    UUID submittedBy,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String reason,
    long version) {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }

  public static HotelReviewRequest submit(HotelReviewSnapshot snapshot, UUID ownerId, Instant now) {
    return new HotelReviewRequest(
        UUID.randomUUID(),
        snapshot.hotel().id(),
        snapshot,
        Status.PENDING,
        ownerId,
        now,
        null,
        null,
        null,
        0);
  }

  public HotelReviewRequest decide(
      Status decision, UUID adminId, Instant now, String reason, long expectedVersion) {
    if (version != expectedVersion) {
      throw new HotelException(HotelErrorCode.REVIEW_CONCURRENT_UPDATE);
    }
    if (status != Status.PENDING) {
      throw new HotelException(HotelErrorCode.REVIEW_ALREADY_DECIDED);
    }
    if (decision != Status.APPROVED && decision != Status.REJECTED) {
      throw HotelException.invalid("Quyết định xét duyệt không hợp lệ.");
    }
    String normalizedReason = reason == null ? null : reason.strip();
    if (decision == Status.REJECTED && (normalizedReason == null || normalizedReason.isBlank())) {
      throw HotelException.invalid("Lý do từ chối là bắt buộc.");
    }
    return new HotelReviewRequest(
        id,
        hotelId,
        snapshot,
        decision,
        submittedBy,
        submittedAt,
        adminId,
        now,
        decision == Status.REJECTED ? normalizedReason : null,
        version);
  }
}
