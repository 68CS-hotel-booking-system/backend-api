package com.team.hotelbooking.booking.review.application.command;

import java.util.UUID;

public record ReplyGuestReviewCommand(
    UUID actorId, UUID hotelId, UUID reviewId, String content, Long expectedVersion) {}
