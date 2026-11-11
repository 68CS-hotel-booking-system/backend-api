package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot;
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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hotel_review_requests")
@Getter
@Setter
@NoArgsConstructor
public class HotelReviewRequestJpaEntity {
  @Id private UUID id;

  @Column(name = "hotel_id", nullable = false, updatable = false)
  private UUID hotelId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
  private HotelReviewSnapshot snapshot;

  // Tên từ snapshot, kể cả Owner đã đổi tên sau khi hồ sơ bị từ chối.
  @Formula("snapshot -> 'hotel' ->> 'name'")
  private String hotelName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private HotelReviewRequest.Status status;

  @Column(name = "submitted_by", nullable = false, updatable = false)
  private UUID submittedBy;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(columnDefinition = "text")
  private String reason;

  @Version
  @Column(nullable = false)
  private Long version;
}
