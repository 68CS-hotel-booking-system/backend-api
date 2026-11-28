package com.team.hotelbooking.booking.review.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.review.application.command.*;
import com.team.hotelbooking.booking.review.application.port.GuestReviewHotelPort;
import com.team.hotelbooking.booking.review.application.result.*;
import com.team.hotelbooking.booking.review.application.usecase.*;
import com.team.hotelbooking.booking.review.domain.exception.*;
import com.team.hotelbooking.booking.review.domain.model.GuestReview;
import com.team.hotelbooking.booking.review.domain.repository.GuestReviewRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class GuestReviewService
    implements SubmitGuestReviewUseCase,
        GetBookingGuestReviewUseCase,
        ListGuestReviewsUseCase,
        ReplyGuestReviewUseCase {
  private final BookingRepository bookings;
  private final GuestReviewRepository reviews;
  private final BookingIdentityPort accounts;
  private final GuestReviewHotelPort hotels;
  private final Clock clock;
  private final int maxSize;

  public GuestReviewService(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      int maxSize) {
    this.bookings = bookings;
    this.reviews = reviews;
    this.accounts = accounts;
    this.hotels = hotels;
    this.clock = clock;
    this.maxSize = maxSize;
  }

  private BookingIdentityPort.AccountSnapshot account(UUID id) {
    if (id == null) throw new GuestReviewException(GuestReviewErrorCode.ACCESS_DENIED);
    return accounts
        .findById(id)
        .filter(a -> a.active() && a.emailVerified())
        .orElseThrow(() -> new GuestReviewException(GuestReviewErrorCode.ACCESS_DENIED));
  }

  private Booking owned(UUID actor, UUID id, boolean lock) {
    if (id == null) throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    return (lock ? bookings.lockById(id) : bookings.findById(id))
        .filter(b -> actor.equals(b.customerId()))
        .orElseThrow(() -> new GuestReviewException(GuestReviewErrorCode.BOOKING_NOT_FOUND));
  }

  public GuestReviewResult execute(SubmitGuestReviewCommand c) {
    if (c == null) throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    var account = account(c.actorId());
    var booking = owned(c.actorId(), c.bookingId(), true);
    if (booking.status() != Booking.Status.CHECKED_OUT)
      throw new GuestReviewException(GuestReviewErrorCode.BOOKING_NOT_REVIEWABLE);
    if (reviews.findByBookingId(booking.id()).isPresent())
      throw new GuestReviewException(GuestReviewErrorCode.GUEST_REVIEW_ALREADY_EXISTS);
    var review =
        GuestReview.submit(
            booking.id(),
            c.actorId(),
            booking.hotelId(),
            booking.bookingCode(),
            account.fullName(),
            c.rating(),
            c.content(),
            clock.instant().truncatedTo(ChronoUnit.MICROS));
    return GuestReviewResult.from(reviews.add(review), false);
  }

  public BookingGuestReviewResult execute(GetBookingGuestReviewQuery q) {
    if (q == null) throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    account(q.actorId());
    var booking = owned(q.actorId(), q.bookingId(), false);
    var review = reviews.findByBookingId(booking.id());
    return new BookingGuestReviewResult(
        booking.status() == Booking.Status.CHECKED_OUT && review.isEmpty(),
        review.isPresent()
            ? "Bạn đã đánh giá đơn này."
            : booking.status() == Booking.Status.CHECKED_OUT
                ? "Chia sẻ trải nghiệm sau lưu trú của bạn."
                : "Bạn có thể đánh giá sau khi trả phòng.",
        review.map(r -> GuestReviewResult.from(r, false)).orElse(null));
  }

  private boolean management(UUID actor, GuestReviewHotelPort.Hotel hotel) {
    var a = account(actor);
    boolean admin = a.roles().contains("ADMIN");
    boolean owner = a.roles().contains("HOTEL_OWNER") && actor.equals(hotel.ownerId());
    if (!admin && !owner) throw new GuestReviewException(GuestReviewErrorCode.ACCESS_DENIED);
    return owner && !admin;
  }

  public ListGuestReviewsResult execute(ListGuestReviewsQuery q) {
    if (q == null
        || q.hotelId() == null
        || q.pagination() == null
        || q.pagination().page() < 0
        || q.pagination().size() < 1
        || q.pagination().size() > maxSize
        || q.rating() != null && (q.rating() < 1 || q.rating() > 5))
      throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    var hotel =
        hotels
            .findById(q.hotelId())
            .filter(h -> q.management() || h.active())
            .orElseThrow(() -> new GuestReviewException(GuestReviewErrorCode.HOTEL_NOT_FOUND));
    boolean canReply = q.management() && management(q.actorId(), hotel);
    var summary = reviews.summarize(q.hotelId());
    return new ListGuestReviewsResult(
        reviews
            .list(q.hotelId(), q.rating(), q.replied(), q.pagination())
            .map(r -> GuestReviewResult.from(r, canReply)),
        summary.totalReviews(),
        summary.averageRating(),
        summary.ratingCounts().stream()
            .map(r -> new ListGuestReviewsResult.RatingCount(r.rating(), r.count()))
            .toList());
  }

  public GuestReviewResult execute(ReplyGuestReviewCommand c) {
    if (c == null
        || c.hotelId() == null
        || c.reviewId() == null
        || c.expectedVersion() == null
        || c.expectedVersion() < 0)
      throw new GuestReviewException(GuestReviewErrorCode.VALIDATION_FAILED);
    var hotel =
        hotels
            .findById(c.hotelId())
            .orElseThrow(() -> new GuestReviewException(GuestReviewErrorCode.HOTEL_NOT_FOUND));
    if (!management(c.actorId(), hotel))
      throw new GuestReviewException(GuestReviewErrorCode.ACCESS_DENIED);
    var review =
        reviews
            .findById(c.reviewId())
            .filter(r -> r.hotelId().equals(c.hotelId()))
            .orElseThrow(
                () -> new GuestReviewException(GuestReviewErrorCode.GUEST_REVIEW_NOT_FOUND));
    if (review.version() != c.expectedVersion())
      throw new GuestReviewException(GuestReviewErrorCode.GUEST_REVIEW_VERSION_CONFLICT);
    var changed =
        review.respond(c.actorId(), c.content(), clock.instant().truncatedTo(ChronoUnit.MICROS));
    if (!reviews.updateReply(changed, review.version()))
      throw new GuestReviewException(GuestReviewErrorCode.GUEST_REVIEW_VERSION_CONFLICT);
    return GuestReviewResult.from(changed, true);
  }
}
