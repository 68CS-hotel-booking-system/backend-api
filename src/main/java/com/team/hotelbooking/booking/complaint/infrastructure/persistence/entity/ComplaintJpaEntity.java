package com.team.hotelbooking.booking.complaint.infrastructure.persistence.entity;

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

// Bản ghi mới được chèn bằng truy vấn ON CONFLICT trong adapter; entity dùng để đọc.
@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
public class ComplaintJpaEntity {

  public enum DbCategory {
    ROOM_CONDITION,
    SERVICE_QUALITY,
    STAFF_BEHAVIOR,
    BILLING,
    CANCELLATION_REFUND,
    OFF_PLATFORM_PAYMENT,
    OTHER
  }

  public enum DbStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    REJECTED
  }

  @Id private UUID id;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "complaint_category__t")
  private DbCategory category;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "complaint_status__t")
  private DbStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;
}
