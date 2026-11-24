package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCommissionDirectory;
import com.team.hotelbooking.booking.domain.model.CommissionAmounts;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCommissionStoreAdapter implements BookingCommissionDirectory {
  private final JdbcTemplate jdbc;

  @Override
  public void save(
      UUID bookingId,
      UUID contractId,
      UUID ownerId,
      BigDecimal rate,
      long roomAmount,
      long discountAmount,
      long totalAmount,
      Instant at) {
    var amounts = CommissionAmounts.calculate(roomAmount, discountAmount, totalAmount, rate);
    jdbc.update(
        """
        INSERT INTO booking_commissions (booking_id, contract_id, owner_user_id, commission_rate,
          basis_amount, platform_amount, owner_amount, captured_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        bookingId,
        contractId,
        ownerId,
        rate,
        amounts.basis(),
        amounts.platform(),
        amounts.owner(),
        java.sql.Timestamp.from(at));
  }
}
