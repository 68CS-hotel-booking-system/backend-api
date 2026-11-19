package com.team.hotelbooking.inventory.infrastructure.persistence.entity;

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
@Table(name = "booking_stay_operations")
@Getter
@Setter
@NoArgsConstructor
public class BookingStayOperationJpaEntity {

  public enum DbBookingStayStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    NO_SHOW
  }

  @Id
  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "stay_status", nullable = false, columnDefinition = "booking_stay_status__t")
  private DbBookingStayStatus stayStatus = DbBookingStayStatus.CONFIRMED;

  @Column(name = "late_arrival_expected_at")
  private Instant lateArrivalExpectedAt;

  @Column(name = "late_arrival_note", length = 500)
  private String lateArrivalNote;

  @Column(name = "late_arrival_recorded_by")
  private UUID lateArrivalRecordedBy;

  @Column(name = "late_arrival_recorded_at")
  private Instant lateArrivalRecordedAt;

  @Column(name = "checked_in_by")
  private UUID checkedInBy;

  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  @Column(name = "checked_out_by")
  private UUID checkedOutBy;

  @Column(name = "checked_out_at")
  private Instant checkedOutAt;

  @Column(name = "no_show_reason", length = 500)
  private String noShowReason;

  @Column(name = "no_show_marked_by")
  private UUID noShowMarkedBy;

  @Column(name = "no_show_marked_at")
  private Instant noShowMarkedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
