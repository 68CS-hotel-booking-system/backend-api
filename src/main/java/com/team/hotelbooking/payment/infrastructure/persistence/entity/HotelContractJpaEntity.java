package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hotel_contracts")
@Getter
@Setter
@NoArgsConstructor
public class HotelContractJpaEntity {

  public enum DbCommissionBasis {
    BEFORE_DISCOUNT,
    AFTER_DISCOUNT,
    AFTER_HOTEL_DISCOUNT
  }

  public enum DbContractStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    TERMINATED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "contract_code", nullable = false, unique = true, length = 100)
  private String contractCode;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal commissionRate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "commission_basis", nullable = false, columnDefinition = "commission_basis__t")
  private DbCommissionBasis commissionBasis;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "hotel_contract_status__t")
  private DbContractStatus status = DbContractStatus.DRAFT;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
