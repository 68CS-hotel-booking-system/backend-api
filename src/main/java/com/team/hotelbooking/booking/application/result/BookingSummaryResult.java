package com.team.hotelbooking.booking.application.result;

import com.team.hotelbooking.booking.domain.model.Booking;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingSummaryResult(
    UUID id,
    String bookingCode,
    UUID customerId,
    UUID hotelId,
    String bookerName,
    String guestName,
    LocalDate checkIn,
    LocalDate checkOut,
    int days,
    int nights,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    int adults,
    int children,
    String status,
    String currency,
    long roomSubtotalAmount,
    long surchargeAmount,
    long subtotalAmount,
    long discountAmount,
    long totalAmount,
    Instant expiresAt,
    Instant createdAt) {

  public static BookingSummaryResult from(Booking booking) {
    return new BookingSummaryResult(
        booking.id(),
        booking.bookingCode(),
        booking.customerId(),
        booking.hotelId(),
        booking.booker().fullName(),
        booking.guest().fullName(),
        booking.checkIn(),
        booking.checkOut(),
        booking.days(),
        booking.nights(),
        booking.expectedCheckInTime(),
        booking.expectedCheckOutTime(),
        booking.adults(),
        booking.children(),
        booking.status().name(),
        booking.currency(),
        booking.roomSubtotalAmount(),
        booking.surchargeAmount(),
        booking.subtotalAmount(),
        booking.discountAmount(),
        booking.totalAmount(),
        booking.expiresAt(),
        booking.createdAt());
  }
}
