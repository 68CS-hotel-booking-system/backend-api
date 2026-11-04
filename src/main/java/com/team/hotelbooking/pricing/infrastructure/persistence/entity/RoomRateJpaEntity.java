package com.team.hotelbooking.pricing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
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

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public RoomRateJpaEntity(RoomRateIdEmbeddable id, Long amount, String currency) {
    this.id = id;
    this.amount = amount;
    this.currency = currency;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }
}
