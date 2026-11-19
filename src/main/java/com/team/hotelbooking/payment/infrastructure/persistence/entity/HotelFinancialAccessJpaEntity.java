package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hotel_financial_access")
@Getter
@Setter
@NoArgsConstructor
public class HotelFinancialAccessJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "is_granted", nullable = false)
  private boolean granted = false;

  @Column(name = "access_scope", nullable = false, length = 50)
  private String accessScope = "FINANCE_READ";

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(name = "granted_by_user_id", nullable = false)
  private UUID grantedByUserId;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt = Instant.now();

  @Version
  @Column(nullable = false)
  private Long version = 0L;
}
