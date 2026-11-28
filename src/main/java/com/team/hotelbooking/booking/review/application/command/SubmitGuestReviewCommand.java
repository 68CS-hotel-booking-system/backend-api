package com.team.hotelbooking.booking.review.application.command;

import java.util.UUID;

public record SubmitGuestReviewCommand(
    UUID actorId, UUID bookingId, Integer rating, String content) {}
