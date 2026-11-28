package com.team.hotelbooking;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class BookableContractFixture {
  private BookableContractFixture() {}

  static void ensure(JdbcTemplate jdbc, UUID hotelId) {
    // Fixture nghiệp vụ mở bán: mỗi khách sạn thử nghiệm phải có hợp đồng riêng.
    jdbc.update(
        """
        INSERT INTO hotel_contracts (id, contract_code, hotel_id, owner_user_id, commission_rate,
          commission_basis, starts_at, ends_at, status, created_by_user_id)
        SELECT ?, ?, h.id, h.owner_user_id, 10, 'AFTER_DISCOUNT',
          '2020-01-01T00:00:00Z', '2099-01-01T00:00:00Z', 'ACTIVE', h.owner_user_id
        FROM hotels h WHERE h.id = ?
        AND NOT EXISTS (SELECT 1 FROM hotel_contracts c WHERE c.hotel_id = h.id AND c.status = 'ACTIVE')
        """,
        UUID.randomUUID(),
        "test-" + UUID.randomUUID(),
        hotelId);
  }
}
