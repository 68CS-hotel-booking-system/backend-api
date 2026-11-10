package com.team.hotelbooking.payment.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface PaymentBookingPort {

  Optional<BookingPaymentSnapshot> findBookingForPayment(UUID bookingId);

  record BookingPaymentSnapshot(
      UUID bookingId,
      String bookingCode,
      UUID customerId,
      UUID hotelId,
      String bookerName,
      String bookerEmail,
      String bookerPhone,
      String guestName,
      String guestEmail,
      String guestPhone,
      LocalDate checkIn,
      LocalDate checkOut,
      int days,
      int nights,
      LocalTime expectedCheckInTime,
      LocalTime expectedCheckOutTime,
      long roomSubtotalAmount,
      long surchargeAmount,
      long subtotalAmount,
      long discountAmount,
      long payableAmount,
      String currency,
      String status,
      Instant holdExpiresAt) {}
}
