package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
public class RoomTypeJpaEntity {

  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "max_adults", nullable = false)
  private short maxAdults = 2;

  @Column(name = "max_children", nullable = false)
  private short maxChildren = 0;

  @Column(name = "max_occupancy", nullable = false)
  private short maxOccupancy = 2;

  @Column(name = "base_price", nullable = false)
  private long basePrice = 0L;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Version
  @Column(nullable = false)
  private Long version = 0L;
}
