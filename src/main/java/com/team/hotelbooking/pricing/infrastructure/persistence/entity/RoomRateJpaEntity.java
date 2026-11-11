package com.team.hotelbooking.pricing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_rates")
@Getter
@Setter
@NoArgsConstructor
public class RoomRateJpaEntity {

  @EmbeddedId private RoomRateIdEmbeddable id;

  @Column(nullable = false)
  private Long amount;

  @Column(nullable = false, length = 3)
  private String currency = "VND";

  @Column(name = "updated_by_user_id")
  private UUID updatedByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public RoomRateJpaEntity(RoomRateIdEmbeddable id, Long amount, String currency) {
    this.id = id;
    this.amount = amount;
    this.currency = currency;
  }
}
