package com.team.hotelbooking.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Hợp đồng hoa hồng đang có hiệu lực cho module khác (booking chụp snapshot khi chốt đơn, PM-15).
 * Có hiệu lực = ACTIVE và starts_at <= at < ends_at; tối đa một hợp đồng nhờ ràng buộc exclusion.
 * Rỗng khi khách sạn chưa có hợp đồng hiệu lực tại thời điểm đó.
 */
public interface ContractDirectory {

  Optional<ContractSnapshot> findEffective(UUID hotelId, Instant at);

  record ContractSnapshot(
      UUID contractId,
      UUID ownerUserId,
      BigDecimal commissionRate,
      CommissionBasis commissionBasis) {}

  enum CommissionBasis {
    BEFORE_DISCOUNT,
    AFTER_DISCOUNT,
    AFTER_HOTEL_DISCOUNT
  }
}
