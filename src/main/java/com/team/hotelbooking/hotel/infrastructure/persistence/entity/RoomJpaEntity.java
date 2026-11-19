package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

  public enum DbReadiness {
    READY,
    DIRTY
  }

  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "room_number", nullable = false, length = 50)
  private String roomNumber;

  @Column(length = 20)
  private String floor;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "room_readiness__t")
  private DbReadiness readiness = DbReadiness.READY;

  @Column(name = "readiness_updated_at", nullable = false)
  private Instant readinessUpdatedAt = Instant.now();

  @Column(name = "readiness_updated_by")
  private UUID readinessUpdatedBy;

  @Column(name = "operational_from", nullable = false)
  private Instant operationalFrom = Instant.now();

  @Column(name = "operational_until")
  private Instant operationalUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Version
  @Column(nullable = false)
  private Long version = 0L;
}
