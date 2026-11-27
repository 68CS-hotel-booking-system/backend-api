package com.team.hotelbooking.booking.review.application.result;

public record BookingGuestReviewResult(
    boolean canReview, String message, GuestReviewResult review) {}
