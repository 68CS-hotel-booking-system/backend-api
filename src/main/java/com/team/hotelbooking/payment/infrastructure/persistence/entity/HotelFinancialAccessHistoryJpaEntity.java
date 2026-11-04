package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hotel_financial_access_history")
@Getter
@Setter
@NoArgsConstructor
public class HotelFinancialAccessHistoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "financial_access_id", nullable = false)
  private UUID financialAccessId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "is_granted", nullable = false)
  private boolean granted;

  @Column(name = "access_scope", length = 50)
  private String accessScope;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(nullable = false)
  private Long version;

  @Column(name = "changed_by_user_id", nullable = false)
  private UUID changedByUserId;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt = Instant.now();
}
