package com.team.hotelbooking.booking.review.application.command;

import java.util.UUID;

public record GetBookingGuestReviewQuery(UUID actorId, UUID bookingId) {}
