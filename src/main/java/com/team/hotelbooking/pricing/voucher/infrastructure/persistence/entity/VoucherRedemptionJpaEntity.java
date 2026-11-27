package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "voucher_redemptions")
@Getter
@Setter
@NoArgsConstructor
public class VoucherRedemptionJpaEntity {
  @Id private UUID bookingId;
  private UUID voucherId;
  private String code;
  private String discountType;
  private long discountValue;
  private Long maxDiscountAmount;
  private long minOrderAmount;
  private long roomAmount;
  private long discountAmount;
  private Instant expiresAt;
  private Instant createdAt;
  private String status;
}
