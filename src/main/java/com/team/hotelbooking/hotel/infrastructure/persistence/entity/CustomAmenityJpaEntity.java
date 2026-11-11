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
@Table(name = "custom_amenities")
@Getter
@Setter
@NoArgsConstructor
public class CustomAmenityJpaEntity {
  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "room_id")
  private UUID roomId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "normalized_name", nullable = false, length = 255)
  private String normalizedName;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false)
  private boolean attached;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;
}
