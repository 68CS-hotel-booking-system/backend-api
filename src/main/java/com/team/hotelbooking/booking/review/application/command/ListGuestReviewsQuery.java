package com.team.hotelbooking.booking.review.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListGuestReviewsQuery(
    UUID actorId,
    UUID hotelId,
    boolean management,
    Integer rating,
    Boolean replied,
    PageQuery pagination) {}
