package com.team.hotelbooking.booking.infrastructure.persistence.entity;

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
@Table(name = "booking_status_history")
@Getter
@Setter
@NoArgsConstructor
public class BookingStatusHistoryJpaEntity {

  @Id private UUID id;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "from_status", columnDefinition = "booking_status__t")
  private BookingJpaEntity.DbStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "to_status", nullable = false, columnDefinition = "booking_status__t")
  private BookingJpaEntity.DbStatus toStatus;

  @Column(name = "changed_by_user_id")
  private UUID changedByUserId;

  @Column(length = 255)
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
