package com.team.hotelbooking.booking.review.infrastructure.persistence.mapper;

import com.team.hotelbooking.booking.review.domain.model.GuestReview;
import com.team.hotelbooking.booking.review.infrastructure.persistence.entity.GuestReviewJpaEntity;

public final class GuestReviewPersistenceMapper {
  private GuestReviewPersistenceMapper() {}

  public static GuestReview toDomain(GuestReviewJpaEntity e) {
    return new GuestReview(
        e.getId(),
        e.getBookingId(),
        e.getCustomerId(),
        e.getHotelId(),
        e.getBookingCode(),
        e.getAuthorDisplayName(),
        e.getRating(),
        e.getContent(),
        e.getCreatedAt(),
        e.getReply(),
        e.getRepliedBy(),
        e.getRepliedAt(),
        e.getVersion());
  }

  public static GuestReviewJpaEntity toEntity(GuestReview r) {
    var e = new GuestReviewJpaEntity();
    e.setId(r.id());
    e.setBookingId(r.bookingId());
    e.setCustomerId(r.customerId());
    e.setHotelId(r.hotelId());
    e.setBookingCode(r.bookingCode());
    e.setAuthorDisplayName(r.authorDisplayName());
    e.setRating((short) r.rating());
    e.setContent(r.content());
    e.setCreatedAt(r.createdAt());
    e.setReply(r.reply());
    e.setRepliedBy(r.repliedBy());
    e.setRepliedAt(r.repliedAt());
    e.setVersion(r.version());
    return e;
  }
}
