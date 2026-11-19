package com.team.hotelbooking.media.infrastructure.persistence.entity;

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
@Table(name = "hotel_images")
@Getter
@Setter
@NoArgsConstructor
public class HotelImageJpaEntity {

  @EmbeddedId private HotelImageId id;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "album_id")
  private UUID albumId;

  @Column(name = "is_cover", nullable = false)
  private boolean cover;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
