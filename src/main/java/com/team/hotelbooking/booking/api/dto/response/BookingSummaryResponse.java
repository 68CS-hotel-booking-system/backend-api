package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingSummaryResponse(
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

  public static BookingSummaryResponse from(BookingSummaryResult result) {
    return new BookingSummaryResponse(
        result.id(),
        result.bookingCode(),
        result.customerId(),
        result.hotelId(),
        result.bookerName(),
        result.guestName(),
        result.checkIn(),
        result.checkOut(),
        result.days(),
        result.nights(),
        result.expectedCheckInTime(),
        result.expectedCheckOutTime(),
        result.adults(),
        result.children(),
        result.status(),
        result.currency(),
        result.roomSubtotalAmount(),
        result.surchargeAmount(),
        result.subtotalAmount(),
        result.discountAmount(),
        result.totalAmount(),
        result.expiresAt(),
        result.createdAt());
  }

  public static PageResponse<BookingSummaryResponse> from(PageResult<BookingSummaryResult> result) {
    return PageResponse.from(result.map(BookingSummaryResponse::from));
  }
}
