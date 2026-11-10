package com.team.hotelbooking.media.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hotel_image_albums")
@Getter
@Setter
@NoArgsConstructor
public class HotelImageAlbumJpaEntity {

  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
