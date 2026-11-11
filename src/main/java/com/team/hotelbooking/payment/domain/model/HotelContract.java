package com.team.hotelbooking.payment.domain.model;

import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Hợp đồng hoa hồng (PM-16). Điều khoản (tỷ lệ, căn cứ, Owner, khoảng hiệu lực) không đổi sau khi
 * tạo; muốn đổi thì tạo hợp đồng mới. Chỉ trạng thái được chuyển: DRAFT → ACTIVE, DRAFT/ACTIVE →
 * TERMINATED.
 */
public record HotelContract(
    UUID id,
    String contractCode,
    UUID hotelId,
    UUID ownerUserId,
    BigDecimal commissionRate,
    CommissionBasis commissionBasis,
    Instant startsAt,
    Instant endsAt,
    ContractStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {

  public enum CommissionBasis {
    BEFORE_DISCOUNT,
    AFTER_DISCOUNT,
    AFTER_HOTEL_DISCOUNT
  }

  public enum ContractStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    TERMINATED
  }

  // Không kích hoạt hợp đồng đã hết hiệu lực; chồng khoảng với hợp đồng ACTIVE khác kiểm ở service
  // và ràng buộc DB.
  public HotelContract activate(Instant now) {
    if (status != ContractStatus.DRAFT) {
      throw new FinanceException(
          FinanceErrorCode.INVALID_CONTRACT_STATUS_TRANSITION,
          "Chỉ hợp đồng ở trạng thái DRAFT mới được kích hoạt.");
    }
    if (!endsAt.isAfter(now)) {
      throw FinanceException.invalidPeriod("Hợp đồng đã hết hiệu lực, không thể kích hoạt.");
    }
    return withStatus(ContractStatus.ACTIVE, now);
  }

  // Chấm dứt không sửa điều khoản hay khoảng hiệu lực; booking đã chốt giữ snapshot cũ.
  public HotelContract terminate(Instant now) {
    if (status != ContractStatus.DRAFT && status != ContractStatus.ACTIVE) {
      throw new FinanceException(
          FinanceErrorCode.INVALID_CONTRACT_STATUS_TRANSITION,
          "Chỉ hợp đồng DRAFT hoặc ACTIVE mới được chấm dứt.");
    }
    return withStatus(ContractStatus.TERMINATED, now);
  }

  private HotelContract withStatus(ContractStatus nextStatus, Instant now) {
    return new HotelContract(
        id,
        contractCode,
        hotelId,
        ownerUserId,
        commissionRate,
        commissionBasis,
        startsAt,
        endsAt,
        nextStatus,
        createdByUserId,
        createdAt,
        now);
  }
}
