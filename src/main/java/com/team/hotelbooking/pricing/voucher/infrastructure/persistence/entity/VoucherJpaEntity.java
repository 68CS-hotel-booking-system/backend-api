package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "hotel_vouchers")
@Getter
@Setter
@NoArgsConstructor
public class VoucherJpaEntity {
  @Id private UUID id;
  private UUID hotelId;
  private String code;
  private String name;
  private String discountType;
  private long discountValue;
  private Long maxDiscountAmount;
  private long minOrderAmount;
  private Instant startsAt;
  private Instant endsAt;
  private int usageLimit;
  private boolean enabled;
  private UUID createdBy;
  private Instant createdAt;
  @Version private Long version;
}
