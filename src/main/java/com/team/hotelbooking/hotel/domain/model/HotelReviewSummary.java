package com.team.hotelbooking.hotel.domain.model;

import java.time.Instant;
import java.util.UUID;

public record HotelReviewSummary(
    UUID id,
    UUID hotelId,
    String hotelName,
    HotelReviewRequest.Status status,
    UUID submittedBy,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String reason,
    long version) {}
