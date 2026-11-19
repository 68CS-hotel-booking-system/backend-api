package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hotel_status_history")
@Getter
@Setter
@NoArgsConstructor
public class HotelStatusHistoryJpaEntity {

  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "from_status", nullable = false, columnDefinition = "hotel_status__t")
  private HotelJpaEntity.DbStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "to_status", nullable = false, columnDefinition = "hotel_status__t")
  private HotelJpaEntity.DbStatus toStatus;

  @Column(length = 500)
  private String reason;

  @Column(name = "changed_by_user_id", nullable = false)
  private UUID changedByUserId;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;
}
