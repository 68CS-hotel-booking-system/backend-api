package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
public class HotelJpaEntity {

  public enum DbHotelStatus {
    draft,
    active,
    inactive
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "city_id", nullable = false)
  private UUID cityId;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, unique = true, length = 255)
  private String slug;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 255)
  private String address;

  @Column(precision = 9, scale = 6)
  private Double latitude;

  @Column(precision = 9, scale = 6)
  private Double longitude;

  @Column(name = "star_rating")
  private Short starRating;

  @Column(name = "check_in_time", nullable = false)
  private LocalTime checkInTime = LocalTime.of(14, 0);

  @Column(name = "check_out_time", nullable = false)
  private LocalTime checkOutTime = LocalTime.of(12, 0);

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "hotel_status__t")
  private DbHotelStatus status = DbHotelStatus.draft;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Version
  @Column(nullable = false)
  private Long version = 0L;
}
