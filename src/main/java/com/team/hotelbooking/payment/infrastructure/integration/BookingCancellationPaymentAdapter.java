package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCancellationPaymentPolicy;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCancellationPaymentAdapter implements BookingCancellationPaymentPolicy {
  private final JdbcTemplate jdbc;

  @Override
  public boolean hasPaymentRisk(UUID bookingId, Instant now) {
    return jdbc.queryForObject(
        """
        SELECT EXISTS (SELECT 1 FROM payment_transactions WHERE booking_id = ?
          AND (status = 'SUCCEEDED' OR (status = 'PENDING' AND expires_at > ?)))
        """,
        Boolean.class,
        bookingId,
        java.sql.Timestamp.from(now));
  }
}
