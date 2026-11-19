package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
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

  public enum DbStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    LOCKED
  }

  @Id private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerId;

  @Column(name = "city_id", nullable = false)
  private UUID cityId;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 255)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false, length = 255)
  private String address;

  @Column(name = "contact_phone", length = 20)
  private String contactPhone;

  @Column(name = "contact_email", length = 255)
  private String contactEmail;

  @Column(precision = 9, scale = 6)
  private BigDecimal latitude;

  @Column(precision = 9, scale = 6)
  private BigDecimal longitude;

  @Column(name = "star_rating")
  private Short starRating;

  @Column(name = "check_in_time", nullable = false)
  private LocalTime checkInTime;

  @Column(name = "check_out_time", nullable = false)
  private LocalTime checkOutTime;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "hotel_status__t")
  private DbStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  public UUID getOwnerUserId() {
    return ownerId;
  }

  public void setOwnerUserId(UUID ownerUserId) {
    this.ownerId = ownerUserId;
  }

  public boolean isActive() {
    return status == DbStatus.ACTIVE;
  }
}
