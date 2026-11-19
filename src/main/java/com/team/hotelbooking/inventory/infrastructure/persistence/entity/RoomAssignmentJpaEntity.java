package com.team.hotelbooking.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "room_assignments")
@Getter
@Setter
@NoArgsConstructor
public class RoomAssignmentJpaEntity {

  public enum DbRoomAssignmentStatus {
    ASSIGNED,
    CHECKED_IN,
    CHECKED_OUT,
    RELEASED
  }

  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "booking_item_id", nullable = false)
  private UUID bookingItemId;

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "room_id", nullable = false)
  private UUID roomId;

  @Column(name = "room_number_snapshot", nullable = false, length = 50)
  private String roomNumberSnapshot;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "room_assignment_status__t")
  private DbRoomAssignmentStatus status = DbRoomAssignmentStatus.ASSIGNED;

  @Column(name = "assigned_by_user_id")
  private UUID assignedByUserId;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  @Column(name = "checked_in_by_user_id")
  private UUID checkedInByUserId;

  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  @Column(name = "checked_out_by_user_id")
  private UUID checkedOutByUserId;

  @Column(name = "checked_out_at")
  private Instant checkedOutAt;

  @Column(name = "released_by_user_id")
  private UUID releasedByUserId;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "release_reason", length = 255)
  private String releaseReason;
}
