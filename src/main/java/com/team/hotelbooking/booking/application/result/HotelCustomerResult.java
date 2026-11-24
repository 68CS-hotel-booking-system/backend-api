package com.team.hotelbooking.booking.application.result;

import java.time.Instant;
import java.util.UUID;

public record HotelCustomerResult(
    UUID id,
    String fullName,
    String email,
    String phone,
    long bookingCount,
    long hotelCount,
    long completedBookingCount,
    Instant lastBookedAt) {}
