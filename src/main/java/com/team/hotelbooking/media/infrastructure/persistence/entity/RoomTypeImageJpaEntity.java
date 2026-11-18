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
@Table(name = "room_type_images")
@Getter
@Setter
@NoArgsConstructor
public class RoomTypeImageJpaEntity {
  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "media_object_id", nullable = false)
  private UUID mediaId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
