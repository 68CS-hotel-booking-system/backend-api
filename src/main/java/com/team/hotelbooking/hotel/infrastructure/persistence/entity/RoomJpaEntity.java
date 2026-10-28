package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class RoomJpaEntity {

  public enum DbRoomStatus {
    ready,
    cleaning,
    maintenance
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "room_number", nullable = false, length = 50)
  private String roomNumber;

  private Short floor;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "room_status__t")
  private DbRoomStatus status = DbRoomStatus.ready;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
